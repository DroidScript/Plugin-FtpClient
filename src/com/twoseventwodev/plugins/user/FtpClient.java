
/*
 DroidScript Plugin class.
 FtpClient - TwoSevenTwo Development [Chris Ferrell]
 2015
 */

package com.twoseventwodev.plugins.user;

import android.os.*;
import android.content.*;
import android.util.Log;
import java.io.*;
import java.lang.reflect.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient
{
	public static String TAG = "FtpClient";
	private int iPort = 21;
	private int iTimeout = 15000;
	private Method m_callscript;
	private Object m_parent;
	private FTPClient ftpClient = new FTPClient();
	//Script callbacks.
	private String m_OnServerResponse;
	private String m_OnDirectoryResponse;
	private String sError = "false";

	//Contruct plugin.
	public FtpClient()
	{
		Log.d(TAG, "Creating plugin object");
	}

	//Initialise plugin.
	public void Init(Context ctx, Object parent)
	{
		try
		{
			Log.d(TAG, "Initialising plugin object");

			m_parent = parent;

			//Use reflection to get 'CallScript' method
			Log.d(TAG, "Getting CallScript method");
			m_callscript = parent.getClass().getMethod("CallScript", Bundle.class);
		} 
		catch (Exception e)
		{
			Log.e(TAG, "Failed to Initialise plugin!", e);
		}
	}

	//Call a function in the user's script.
	private void CallScript(Bundle b)
	{
		try
		{
			m_callscript.invoke(m_parent, b);
		} 
		catch (Exception e)
		{
			Log.e(TAG, "Failed to call script function!", e);
		}
	}

	//Handle commands from DroidScript.
	public String CallPlugin(Bundle b)
	{
		String cmd = b.getString("cmd");
		String ret = null;
		try
		{

			if (cmd.equals("getError"))
			{ return sError; }
			sError = "false";

			switch (cmd)
			{
				case "GetVersion": return GetVersion(b);
				case "Connect": return ConnectFtp(b);
				case "Disconnect": return DisconnectFtp(b);
				case "GetDirList": return getDirListing(b);
				case "OnServerResponse": SetOnServerResponse(b); break;
				case "OnDirectoryResponse": SetOnDirectoryResponse(b); break;
				case "SetPort": SetPort(b); break;
				case "SetServerTimeout": SetServerTimeout(b); break;
				case "RenameFile": return ServerActions("renameFile", b);
				case "DeleteFile": return ServerActions("deleteFile", b);
				case "ChangeDir": return ServerActions("changeDir", b);
				case "CreateDir": return ServerActions("createDir", b);
				case "RemoveDir": return ServerActions("removeDir", b);
				case "GetDetails": return getFileDetails(b);
				case "DownloadFile": return TransferFile(b, "down");
				case "UploadFile": return TransferFile(b, "up");
				case "CurrentDir": return ServerActions("currentDir", b);
				case "isConnected": return String.valueOf(ftpClient.isConnected());
				case "uploadDirectory": return Upload_Directory(b);
				case "downloadDirectory": return Download_Directory(b);
				case "fileExists": return doesExist(b, "file");
				case "directoryExists": return doesExist(b, "dir");
				case "forceRemoveDir": return ForceDeleteDirectory(b);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Plugin command failed!", e);
		}
		return ret;
	}

	private String NC()
	{
		SetError("Error: Not Connected"); return "false";	
	}

	private void showServerReply()
	{
		if (m_OnServerResponse != null)
		{	
			Bundle b;		
			String[] replies = ftpClient.getReplyStrings();
			if (replies != null && replies.length > 0)
			{
				for (String aReply : replies)
				{
					b = new Bundle();
					b.putString("cmd", m_OnServerResponse);
					b.putString("p1", "SERVER: " + aReply);
					CallScript(b);
				}
			}
		}
	}

	private void showDirectoryStatus(String resp)
	{
		if (m_OnDirectoryResponse != null)
		{	
			Bundle b; b = new Bundle();
			b.putString("cmd", m_OnDirectoryResponse);
			b.putString("p1", resp);
			CallScript(b);
		}
	}


	private String ServerActions(String action, Bundle b)
	{
		String p1 = b.getString("p1");
		String p2 = b.getString("p2");
		String ret = "false"; boolean success = false;

		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }

			switch (action)
			{	
				case "changeDir": success = ftpClient.changeWorkingDirectory(p1); break;
				case "createDir": success = ftpClient.makeDirectory(p1); break;
				case "renameFile": success = ftpClient.rename(p1, p2); break;
				case "deleteFile": success = ftpClient.deleteFile(p1); break;
				case "removeDir": success = ftpClient.removeDirectory(p1); break;		
				case "currentDir": return ftpClient.printWorkingDirectory();
			}	

			if (success)
			{ ret = "true"; }

		}
		catch (IOException e)
		{
			SetError("Error: " + e);
		}
		showServerReply();
		return ret;
	}

	private String TransferFile(Bundle b, String dir)
	{

		String localFile; String remoteFile; boolean success;

		switch (dir)
		{
			case "up":
				localFile = b.getString("p1");
				remoteFile = b.getString("p2");
				break;
			default:
				remoteFile = b.getString("p1");
				localFile = b.getString("p2");
		}
		String mode = b.getString("p3");
		String ret = "false";

		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }

			switch (dir)
			{
				case "up":
					success = uploadSingleFile(localFile, remoteFile, mode);
					break;
				default:
					success = downloadSingleFile(remoteFile, localFile, mode);
			}

			if (success)
			{ ret = "true"; }
		}
		catch (IOException e)
		{
			SetError("Error: " + e);
		}
		showServerReply();
		return ret;
	}

	private String doesExist(Bundle b, String type)
	{
		String path = b.getString("p1"); boolean success;
		String ret = "false";
		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }

			switch (type)
			{
				case "dir":
					success = checkDirectoryExists(path);
					break;
				default:
					success = checkFileExists(path);
			}

			if (success)
			{ ret = "true"; }
		}
		catch (IOException e)
		{
			SetError("Error: " + e);
		}
		showServerReply();
		return ret;
	}

	private String ForceDeleteDirectory(Bundle b)
	{
		String dir = b.getString("p1");

		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }
			removeDirectory(dir, "");
		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}
		showServerReply();
		return "true";
	}

	private String Upload_Directory(Bundle b)
	{
		String localDir = b.getString("p1");
		String remoteDir = b.getString("p2");
		String mode = b.getString("p3");

		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }
			uploadDirectory(remoteDir, localDir, "", mode);
		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}
		showServerReply();
		return "true";
	}

	private String Download_Directory(Bundle b)
	{
		String remoteDir = b.getString("p1");
		String localDir = b.getString("p2");
		String mode = b.getString("p3");

		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }
			downloadDirectory(remoteDir, "", localDir, mode);
		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}
		showServerReply();
		return "true";
	}

	private String getFileDetails(Bundle b)
	{
		String path = b.getString("p1");
		String ret = "false";
		try
		{
			if (!ftpClient.isConnected())
			{ return NC(); }

			FTPFile ftpFile = ftpClient.mlistFile(path);
			if (ftpFile != null)
			{
				StringBuilder s = new StringBuilder("");
				String timestamp = ftpFile.getTimestamp().getTime().toString();
				String type = ftpFile.isDirectory() ? "Directory" : "File";
				s.append("{");
				s.append("name:\"" + ftpFile.getName()+ "\",");
				s.append("size:" + ftpFile.getSize() + ",");
				s.append("type:\"" + type + "\",");
				s.append("timestamp:\"" + timestamp + "\",");
				s.append("user:\"" + ftpFile.getUser() + "\",");
				s.append("group:\"" + ftpFile.getGroup() + "\"");
				s.append("}");
				ret = s.toString();
			}
			else
			{
				SetError(path + " Not Found"); return "false";
			}
		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}
		showServerReply();
		return ret;
	}

	private boolean checkDirectoryExists(String dirPath) throws IOException
	{
		String cur = ftpClient.printWorkingDirectory();
		ftpClient.changeWorkingDirectory(dirPath);
		int returnCode = ftpClient.getReplyCode();
		if (returnCode == 550)
		{
			return false;
		}
		// if exists switch back to correct working directory
		ftpClient.changeWorkingDirectory(cur);
		return true;
	}

	private boolean checkFileExists(String filePath) throws IOException
	{
		InputStream inputStream = ftpClient.retrieveFileStream(filePath);
		int returnCode = ftpClient.getReplyCode();
		if (inputStream == null || returnCode == 550)
		{
			return false;
		}
		ftpClient.completePendingCommand();
		return true;
	}	

	private String getDirListing(Bundle b)
	{
		String dir = b.getString("p1");
		try
		{
			if (!ftpClient.isConnected())
			{
				return NC();
			}

			FTPFile[] files1 = ftpClient.listFiles(dir);
			return returnFileDetails(files1);

		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}
	}

	private String DisconnectFtp(Bundle b)
	{
		try
		{
			if (ftpClient.isConnected())
			{
				ftpClient.logout();
				ftpClient.disconnect();
				showServerReply();
				return "true";
			}
			else
			{ return "true"; }
		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}	
	}

	private String ConnectFtp(Bundle b)
	{
		String user = b.getString("p1");
		String pass = b.getString("p2");
		String server = b.getString("p3");

		try
		{
			if (ftpClient.isConnected())
			{
				SetError("Still Connected"); return "false";
			}
			Log.d(TAG, "Connecting FTP");
			ftpClient.setConnectTimeout(iTimeout);
			ftpClient.connect(server, iPort);
			showServerReply();

			int replyCode = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(replyCode))
			{
				SetError("Operation failed. Server reply code: " + replyCode); return "false";
			}

			boolean success = ftpClient.login(user, pass);
			showServerReply();

			if (!success)
			{
				SetError("Could not login to the server"); return "false";
			}
			else
			{
				Log.d(TAG, "LOGGED IN SERVER");
				ftpClient.enterLocalPassiveMode();
			}

		}
		catch (IOException e)
		{
			SetError("Error: " + e); return "false";
		}
		return "true";
	}

	private void SetError(String e)
	{
		sError = e;
	}

	private void SetOnServerResponse(Bundle b)
	{
		Log.d(TAG, "Got SetOnServerResponse");
		m_OnServerResponse = b.getString("p1");
	}

	private void SetOnDirectoryResponse(Bundle b)
	{
		Log.d(TAG, "Got SetOnServerResponse");
		m_OnDirectoryResponse = b.getString("p1");
	}

	private void SetPort(Bundle b)
	{
		Log.d(TAG, "Got SetPort");
		iPort = Integer.parseInt(b.getString("p1"));
	}

	private void SetServerTimeout(Bundle b)
	{
		Log.d(TAG, "Got SetTimeout");
		iTimeout = Integer.parseInt(b.getString("p1")) * 1000;
	}

    private String returnFileDetails(FTPFile[] files )
	{
        String ret = "false";
		DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int i = 0; StringBuilder s = new StringBuilder("");
		for (FTPFile file : files)
		{   
			if (i == 0)
			{
				s.append("[");	
			}
			i++;
			if (i > 1) s.append(",");
     		s.append("{");
			String type = file.isDirectory() ? "Directory" : "File";
			s.append("name:\"" + file.getName() + "\",");
			s.append("type:\"" + type + "\",");
			s.append("size:" + file.getSize() + ",");
			s.append("user:\"" + file.getUser() + "\",");
			s.append("group:\"" + file.getGroup() + "\",");
			s.append("timestamp:\"" + file.getTimestamp().getTime().toString() + "\",");
			s.append("datetime:\"" + dateFormater.format(file.getTimestamp().getTime()) + "\"");
			s.append("}");
        }

		if (i > 0)
		{ s.append("]"); ret = s.toString(); }

		showServerReply();
		return ret;
	}

	private String GetVersion(Bundle b)
	{
		Log.d(TAG, "Got GetVersion");
		return "FtpClient DroidScript Plugin\nv1.7 - 2015 by Chris Ferrell";
	}

	private void uploadDirectory(String remoteDirPath, String localParentDir, String remoteParentDir, String mode)
	throws IOException
	{

		showDirectoryStatus("LISTING directory: " + localParentDir);

		File localDir = new File(localParentDir);
		File[] subFiles = localDir.listFiles();
		if (subFiles != null && subFiles.length > 0)
		{
			for (File item : subFiles)
			{
				String remoteFilePath = remoteDirPath + "/" + remoteParentDir
					+ "/" + item.getName();
				if (remoteParentDir.equals(""))
				{
					remoteFilePath = remoteDirPath + "/" + item.getName();
				}


				if (item.isFile())
				{
					// upload the file
					String localFilePath = item.getAbsolutePath();
					showDirectoryStatus("About to upload the file: " + localFilePath);
					boolean uploaded = uploadSingleFile(localFilePath, remoteFilePath, mode);
					if (uploaded)
					{
						showDirectoryStatus("UPLOADED a file to: " + remoteFilePath);
					}
					else
					{
						showDirectoryStatus("COULD NOT upload the file: " + localFilePath);
					}
				}
				else
				{
					// create directory on the server
					boolean created = ftpClient.makeDirectory(remoteFilePath);
					if (created)
					{
						showDirectoryStatus("CREATED the directory: " + remoteFilePath);
					}
					else
					{
						showDirectoryStatus("COULD NOT create the directory: " + remoteFilePath);
					}

					// upload the sub directory
					String parent = remoteParentDir + "/" + item.getName();
					if (remoteParentDir.equals(""))
					{
						parent = item.getName();
					}

					localParentDir = item.getAbsolutePath();
					uploadDirectory(remoteDirPath, localParentDir, parent, mode);
				}
			}
		}
	}

	private boolean uploadSingleFile(String localFilePath, String remoteFilePath, String mode) throws IOException
	{
		File localFile = new File(localFilePath);

		InputStream inputStream = new FileInputStream(localFile);
		try
		{
			if (!mode.equals("ASCII"))
			{
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			}
			else
			{
				ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
			}
			return ftpClient.storeFile(remoteFilePath, inputStream);
		}
		finally
		{
			inputStream.close();
		}
	}

	private void downloadDirectory(String parentDir, String currentDir, String saveDir, String mode) throws IOException
	{
		String dirToList = parentDir;
		if (!currentDir.equals(""))
		{
			dirToList += "/" + currentDir;
		}

		FTPFile[] subFiles = ftpClient.listFiles(dirToList);

		if (subFiles != null && subFiles.length > 0)
		{
			for (FTPFile aFile : subFiles)
			{
				String currentFileName = aFile.getName();
				if (currentFileName.equals(".") || currentFileName.equals(".."))
				{
					// skip parent directory and the directory itself
					continue;
				}
				String filePath = parentDir + "/" + currentDir + "/"
					+ currentFileName;
				if (currentDir.equals(""))
				{
					filePath = parentDir + "/" + currentFileName;
				}

				String newDirPath = saveDir + parentDir + File.separator + currentDir + File.separator + currentFileName;
				if (currentDir.equals(""))
				{
					newDirPath = saveDir + parentDir + File.separator + currentFileName;
				}

				if (aFile.isDirectory())
				{
					// create the directory in saveDir
					File newDir = new File(newDirPath);
					boolean created = newDir.mkdirs();
					if (created)
					{
						showDirectoryStatus("CREATED the directory: " + newDirPath);
					}
					else
					{
						showDirectoryStatus("COULD NOT create the directory: " + newDirPath);
					}

					// download the sub directory
					downloadDirectory(dirToList, currentFileName, saveDir, mode);
				}
				else
				{
					// download the file
					boolean success = downloadSingleFile(filePath, newDirPath, mode);
					if (success)
					{
						showDirectoryStatus("DOWNLOADED the file: " + filePath);
					}
					else
					{
						showDirectoryStatus("COULD NOT download the file: " + filePath);
					}
				}
			}
		}
	}

	private boolean downloadSingleFile(String remoteFilePath, String savePath, String mode) throws IOException
	{
		File downloadFile = new File(savePath);

		File parentDir = downloadFile.getParentFile();
		if (!parentDir.exists())
		{
			parentDir.mkdir();
		}

		OutputStream outputStream = new BufferedOutputStream(
			new FileOutputStream(downloadFile));
		try
		{
			if (!mode.equals("ASCII"))
			{
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			}
			else
			{
				ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
			}
			return ftpClient.retrieveFile(remoteFilePath, outputStream);
		}
		catch (IOException ex)
		{
			throw ex;
		}
		finally
		{
			if (outputStream != null)
			{
				outputStream.close();
			}
		}
	}	

	public void removeDirectory(String parentDir, String currentDir) throws IOException
	{
		String dirToList = parentDir;
		if (!currentDir.equals(""))
		{
			dirToList += "/" + currentDir;
		}

		FTPFile[] subFiles = ftpClient.listFiles(dirToList);

		if (subFiles != null && subFiles.length > 0)
		{
			for (FTPFile aFile : subFiles)
			{
				String currentFileName = aFile.getName();
				if (currentFileName.equals(".") || currentFileName.equals(".."))
				{
					continue;
				}
				String filePath = parentDir + "/" + currentDir + "/"
					+ currentFileName;
				if (currentDir.equals(""))
				{
					filePath = parentDir + "/" + currentFileName;
				}

				if (aFile.isDirectory())
				{
					removeDirectory(dirToList, currentFileName);
				}
				else
				{
					boolean deleted = ftpClient.deleteFile(filePath);
					if (deleted)
					{
						showDirectoryStatus("DELETED the file: " + filePath);
					}
					else
					{
						showDirectoryStatus("CANNOT delete the file: " + filePath);
					}
				}
			}

			boolean removed = ftpClient.removeDirectory(dirToList);
			if (removed)
			{
				showDirectoryStatus("REMOVED the directory: " + dirToList);
			}
			else
			{
				showDirectoryStatus("CANNOT remove the directory: " + dirToList);
			}
		}
	}


} 



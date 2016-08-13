app.CreateFtpClient = function() 
{ 
return new FtpClient();
}

function FtpClient( options )
{
    this.plg = _CreatePlugin( "com.twoseventwodev.plugins.user.FtpClient", options );

	this.Connect = function ( user, pass, server )
	{
	 var b = this.plg.Send( "Connect", user, pass, server );
	 return (b === 'true') ? true : false;
	}

	this.GetError = function ( )
	{
	 var e = this.plg.Send( "getError" );
	 return (e === 'false') ? false : e;
	}

	this.Disconnect = function ( )
	{
	 var b = this.plg.Send( "Disconnect" );
	 return (b === 'true') ? true : false;
	}

	this.GetDirectoryList = function ( dir )
	{
		if (dir === null || dir === undefined) { dir = ''; }
		var list = this.plg.Send( "GetDirList", dir );
		if (list === 'false') { return false; } else
		{ return new Function( 'return (' + list + ')' )(); }
	}

	this.GetFileDetails = function ( file )
	{
		var d = this.plg.Send( "GetDetails", file );
		if (d === 'false') { return false; } else
		{ return new Function( 'return (' + d + ')' )(); }
	}

	this.FileExists = function ( file )
	{
		var b = this.plg.Send( "fileExists", file );
		return (b === 'true') ? true : false;
	}

	this.DirectoryExists = function ( path )
	{
		var b = this.plg.Send( "directoryExists", path );
	 	return (b === 'true') ? true : false;
	}

	this.ChangeWorkingDirectory = function ( dir )
	{
		var b = this.plg.Send( "ChangeDir", dir, null );
		return (b === 'true') ? true : false;
	}

	this.CreateDirectory = function ( dir )
	{
		var b = this.plg.Send( "CreateDir", dir, null );
		return (b === 'true') ? true : false;
	}

	this.GetVersion = function( )
	{
		return this.plg.Send( "GetVersion" ); 
	}
    
 	this.SetOnResponse = function ( cb )
 	{
 		this.plg.Send( "OnServerResponse", cb.name );
 	}

	this.SetOnStatus = function ( cb )
 	{
 		this.plg.Send( "OnDirectoryResponse", cb.name );
 	}

	this.SetPort = function ( num )
 	{
 		this.plg.Send( "SetPort", num );
 	}

	this.SetServerTimeout = function ( num )
 	{
 		this.plg.Send( "SetServerTimeout", num );
 	}

	this.DownloadFile = function ( remote, local, mode )
	{
	 if (mode === null || mode === undefined) { mode = 'BINARY'; }
		var b = this.plg.Send( "DownloadFile", remote, local, mode );
		return (b === 'true') ? true : false;
	}

	this.UploadFile = function ( local, remote, mode )
	{
	 if (mode === null || mode === undefined) { mode = 'BINARY'; }
		var b = this.plg.Send( "UploadFile", local, remote, mode );
		return (b === 'true') ? true : false;
	}

	this.UploadDirectory = function ( local, remote, mode )
	{
	 if (mode === null || mode === undefined) { mode = 'BINARY'; }
		var b = this.plg.Send( "uploadDirectory", local, remote, mode );
	 	return (b === 'true') ? true : false;
	}

	this.DownloadDirectory = function ( remote, local, mode )
	{
	 if (mode === null || mode === undefined) { mode = 'BINARY'; }
		var b = this.plg.Send( "downloadDirectory", remote, local, mode );
	    return (b === 'true') ? true : false;
	}

	this.DeleteFile = function ( file )
	{
		var b = this.plg.Send( "DeleteFile", file, null );
		return (b === 'true') ? true : false;
	}

	this.RemoveDirectory = function ( dir )
	{
		var b = this.plg.Send( "RemoveDir", dir, null );
		return (b === 'true') ? true : false;
	}

	this.ForceRemoveDirectory = function ( dir )
	{
		var b = this.plg.Send( "forceRemoveDir", dir );
		return (b === 'true') ? true : false;
	}

	this.GetCurrentDirectory = function ( )
	{
		return this.plg.Send( "CurrentDir", null, null );
	}

	this.RenameFile = function ( oldFile, newFile )
	{
		var b = this.plg.Send( "RenameFile", oldFile, newFile );
		return (b === 'true') ? true : false;
	}

	this.IsConnected = function ()
	{
		var b = this.plg.Send( "isConnected" );
		return (b === 'true') ? true : false;
	}

}

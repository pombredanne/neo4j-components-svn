package org.neo4j.util.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parses a line from the client with the intention of interpreting it as
 * an "app" command, f.ex. like:
 * 
 * "ls -pf title.* 12"
 * 
 * o ls is the app.
 * o p and f are options, p w/o value and f has the value "title.*"
 *   (defined in {@link App#getOptionValueType(String)}.
 * o 12 is an argument.
 */
public class AppCommandParser
{
	private AppShellServer server;
	private String line;
	private String appName;
	private App app;
	private Map<String, String> options = new HashMap<String, String>();
	private List<String> arguments = new ArrayList<String>();
	
	/**
	 * @param server the server used to find apps.
	 * @param line the line from the client to interpret.
	 * @throws ShellException if there's something wrong with the line.
	 */
	public AppCommandParser( AppShellServer server, String line )
		throws ShellException
	{
		this.server = server;
		if ( line != null )
		{
			line = line.trim();
		}
		this.line = line;
		this.parse();
	}
	
	private void parse() throws ShellException
	{
		if ( this.line == null || this.line.trim().length() == 0 )
		{
			return;
		}
		
		this.parseApp();
		this.parseParameters();
	}
	
	private void parseApp() throws ShellException
	{
		int index = findNextWhiteSpace( this.line, 0 );
		this.appName = index == -1 ?
			this.line : this.line.substring( 0, index );
		try
		{
			this.app = this.server.findApp( this.appName );
		}
		catch ( Exception e )
		{
			throw new ShellException( e );
		}
		if ( this.app == null )
		{
			throw new ShellException(
				"Unknown command '" + this.appName + "'" );
		}
	}
	
	private void parseParameters() throws ShellException
	{
		String rest = this.line.substring( this.appName.length() ).trim();
		String[] parsed = tokenizeStringWithQuotes( rest, false );
		for ( int i = 0; i < parsed.length; i++ )
		{
			String string = parsed[ i ];
			if ( string.startsWith( "--" ) )
			{
				// It is one long name
				String name = string.substring( 2 );
				i = this.fetchArguments( parsed, i, name );
			}
			else if ( this.isOption( string ) )
			{
				String options = string.substring( 1 );
				for ( int o = 0; o < options.length(); o++ )
				{
					String name = String.valueOf( options.charAt( o ) );
					i = this.fetchArguments( parsed, i, name );
				}
			}
			else
			{
				this.arguments.add( string );
			}
		}
	}
	
	private boolean isOption( String string )
	{
		return string.startsWith( "-" );
	}
	
	private int fetchArguments( String[] parsed, int whereAreWe,
		String optionName ) throws ShellException
	{
		String value = null;
		OptionValueType type = this.app.getOptionValueType( optionName );
		if ( type == OptionValueType.MUST )
		{
			whereAreWe++;
			String message = "Value required for '" + optionName + "'";
			this.assertHasIndex( parsed, whereAreWe, message );
			value = parsed[ whereAreWe ];
			if ( this.isOption( value ) )
			{
				throw new ShellException( message );
			}
		}
		else if ( type == OptionValueType.MAY )
		{
			if ( this.hasIndex( parsed, whereAreWe + 1 ) &&
				!this.isOption( parsed[ whereAreWe + 1 ] ) )
			{
				whereAreWe++;
				value = parsed[ whereAreWe ];
			}
		}
		this.options.put( optionName, value );
		return whereAreWe;
	}
	
	private boolean hasIndex( String[] array, int index )
	{
		return index >= 0 && index < array.length;
	}
	
	private void assertHasIndex( String[] array, int index, String message )
		throws ShellException
	{
		if ( !this.hasIndex( array, index ) )
		{
			throw new ShellException( message );
		}
	}
	
	private static int findNextWhiteSpace( String line, int fromIndex )
	{
		int index = line.indexOf( ' ', fromIndex );
		return index == -1 ? line.indexOf( '\t', fromIndex ) : index;
	}
	
	/**
	 * @return the name of the app (from {@link #getLine()}).
	 */
	public String getAppName()
	{
		return this.appName;
	}
	
	/**
	 * @return the app corresponding to the {@link #getAppName()}.
	 */
	public App app()
	{
		return this.app;
	}

	/**
	 * @return the supplied options (from {@link #getLine()}).
	 */
	public Map<String, String> options()
	{
		return this.options;
	}
	
	/**
	 * @return the arguments (from {@link #getLine()}).
	 */
	public List<String> arguments()
	{
		return this.arguments;
	}
	
	/**
	 * @return the entire line from the client.
	 */
	public String getLine()
	{
		return this.line;
	}
	
	/**
	 * @return the line w/o the app (just the options and arguments).
	 */
	public String getLineWithoutCommand()
	{
		return this.line.substring( this.appName.length() ).trim();
	}

	/**
	 * Tokenizes a string, regarding quotes.
	 * @param string the string to tokenize.
	 * @return the tokens from the line.
	 */
	public static String[] tokenizeStringWithQuotes( String string )
	{
		return tokenizeStringWithQuotes( string, true );
	}

	/**
	 * Tokenizes a string, regarding quotes.
	 * @param string the string to tokenize.
	 * @param trim wether or not to trim each token or not.
	 * @return the tokens from the line.
	 */
	public static String[] tokenizeStringWithQuotes( String string,
		boolean trim )
	{
		if ( trim )
		{
			string = string.trim();
		}
		ArrayList<String> result = new ArrayList<String>();
		string = string.trim();
		boolean inside = string.startsWith( "\"" );
		StringTokenizer quoteTokenizer = new StringTokenizer( string, "\"" );
		while ( quoteTokenizer.hasMoreTokens() )
		{
			String token = quoteTokenizer.nextToken();
			if ( trim )
			{
				token = token.trim();
			}
			if ( token.length() == 0 )
			{
				// Skip it
			}
			else if ( inside )
			{
				// Don't split
				result.add( token );
			}
			else
			{
				// Split
				StringTokenizer spaceTokenizer =
					new StringTokenizer( token, " " );
				while ( spaceTokenizer.hasMoreTokens() )
				{
					String spaceToken = spaceTokenizer.nextToken();
					if ( trim )
					{
						spaceToken = spaceToken.trim();
					}
					result.add( spaceToken );
				}
			}
			inside = !inside;
		}
		return result.toArray( new String[ result.size() ] );
	}
}

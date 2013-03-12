package Steps;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;


public class Reader {
	
	private BufferedReader reader;
	
	public Reader(File file) {
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
		} catch(FileNotFoundException ex) {
			System.out.println(ex.getMessage());
		}
		reader = new BufferedReader( new InputStreamReader( input, Charset.forName("UTF-8")));
	}
	
	public int getSym(){ //return current and advance to the next character on the input
		int result = 0;
		try {
			result = reader.read();
		} catch(IOException ex) {
			System.out.println(ex.getMessage());
		}
		return result;
	}
	
	public void nextLine(){
		try {
			reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

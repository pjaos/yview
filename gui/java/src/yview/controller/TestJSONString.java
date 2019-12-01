package yview.controller;

import org.json.JSONObject;

/**
 * @brief Cmd line test pass first argument and convert it to a JSON object
 *        to test for valid json text. USed for dev purposes to check for 
 *        non compliant JSON messages.
 */
public class TestJSONString {

	public static void main(String[] args) {
		new TestJSONString(args);
	}
	
	public TestJSONString(String[] args) {
		System.out.println("STRING TO CONVERT = <"+args[0]+">");
		JSONObject json = new JSONObject(args[0]);
		System.out.println("Converted to JSONObject instance = <"+json+">");
	}

}

package yview.controller;

import org.json.JSONObject;

/**
 * Cmd line test pass first argument and convert it to a JSON object
 * to test for valid json text.
 * @author pja
 *
 */
public class TestJSONString {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("STRING TO CONVERT = <"+args[0]+">");
		JSONObject json = new JSONObject(args[0]);
	}

}

package com.kgl.KglServices.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping({ "/call" })
public class CallrecordingApiController {
	@Value("${CA_URL}")
	private String CA_URLs;

	@Value("${NA_URL}")
	private String NA_URLs;

	@Value("${RLA_URL}")
	private String RLA_URLs;

	@Value("${KA_URL}")
	private String KA_URLs;

	@Value("${GJ_URL}")
	private String GJ_URLs;

	@Value("${TS_URL}")
	private String TS_URLs;

	@Value("${TN_URL}")
	private String TN_URLs;

	@Value("${HO_CALL_URL}")
	private String HO_CALL_URLs;

	@Value("${ExotelUrl}")
	private String ExotelUrl;

	@Value("${Exotel_sid_URL}")
	private String Exotel_sid_URL;

	@Value("${Exotel_auth_token}")
	private String Exotel_auth_token;

	private static final Logger logger = LoggerFactory.getLogger(CallrecordingApiController.class);

	@GetMapping("/testApi")
	public String test() {
		return "hi";
	}

	@PostMapping("/api")
	public ResponseEntity<String> recordingApi(@RequestBody String jsonRequestData)
			throws ParseException, InterruptedException {
		JSONParser jsonParser = new JSONParser();
		Object obj = jsonParser.parse(jsonRequestData);
		JSONObject rawJsonData = (JSONObject) obj;
		String tid = (String) rawJsonData.get("TID");
		String From = (String) rawJsonData.get("HO_CREDIT_PH");
		String To = (String) rawJsonData.get("CUST_PH_NO");
		String table = (String) rawJsonData.get("TABLE");
		String CallerId = (String) rawJsonData.get("EXOTEL_NO");
		ResponseEntity<String> status = null;
		String sid = null;
		logger.info("....starts here....");
		logger.info("TID:::  " + tid + " table " + table + " from  " + From + " To  " + To + " CallerId  " + CallerId);
		sid = getData(tid, From, To, CallerId);
		if (!sid.isEmpty()) {
			status = updateIntoAppSheet(tid, sid, table, "", "");
		} else {
			logger.info("TID not found");
		}
		logger.info(":::::call ends here::::::");
		return status;
	}

	private String getData(String tid, String From, String To, String CallerId) throws ParseException {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("From", From);
		map.add("To", To);
		map.add("CallerId", CallerId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", Exotel_auth_token);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> restTemplate = new RestTemplate().exchange(ExotelUrl, HttpMethod.POST, entity,
				String.class);
		String sid = getSidFromRestTemplate(restTemplate.getBody());
		return sid;
	}

	private String getSidFromRestTemplate(String body) {
		// TODO Auto-generated method stub
		String sid = null;
		Pattern pattern = Pattern.compile("<Sid>(.*)</Sid>");
		Matcher matcher = pattern.matcher(body);
		if (matcher.find()) {
			sid = matcher.group(1);
		}
		return sid;
	}

	@PostMapping("/sid")
	public String urlBySid(@RequestBody String reqBody) throws ParseException, InterruptedException {
		JSONParser jsonParser = new JSONParser();
		Object obj = jsonParser.parse(reqBody);
		JSONObject rawJsonData = (JSONObject) obj;
		String sid = (String) rawJsonData.get("SID");
		logger.info("....SID API starts here...." + sid);
		String tid = (String) rawJsonData.get("TID");
		String table_name = (String) rawJsonData.get("TABLE");
		String SID_URL = Exotel_sid_URL + sid;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", Exotel_auth_token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> restTemplate = new RestTemplate().exchange(SID_URL, HttpMethod.GET, entity,
				String.class);
		Map<String, String> respData = getCallDataBySid(restTemplate.getBody());
		String call_url = respData.get("Call_URL");
		String call_status = respData.get("Call_Status");
		updateIntoAppSheet(tid, sid, table_name, call_url, call_status);
		logger.info("....SID API ends here....");
		return "hi";
	}
	
	private Map<String, String> getCallDataBySid(String body) {
		String call_link = null;
		String status = null;
		Pattern pattern = Pattern.compile("<RecordingUrl>(.*)</RecordingUrl>");
		Pattern pattern2 = Pattern.compile("<Status>(.*)</Status>");
		Matcher matcher = pattern.matcher(body);
		Matcher matcher2 = pattern2.matcher(body);
		if (matcher.find() && matcher2.find()) {
			call_link = matcher.group(1);
			status = matcher2.group(1);
		}
		logger.info("Call_URL" + call_link);
		logger.info("Status" + status);
		Map<String, String> renderObj = new HashMap<String, String>();
		renderObj.put("Call_URL", call_link);
		renderObj.put("Call_Status", status);
		return renderObj;
	}
	
	private ResponseEntity<String> updateIntoAppSheet(String tid, String sid, String table, String Call_url,
			String call_status) {
		String url = null;
		if (table.equalsIgnoreCase("REC_CA")) {
			url = CA_URLs;
		} else if (table.equalsIgnoreCase("REC_NA")) {
			url = NA_URLs;
		} else if (table.equalsIgnoreCase("REC_RAL")) {
			url = RLA_URLs;
		} else if (table.equalsIgnoreCase("REC_KA")) {
			url = KA_URLs;
		} else if (table.equalsIgnoreCase("REC_GJ")) {
			url = GJ_URLs;
		} else if (table.equalsIgnoreCase("REC_TS")) {
			url = TS_URLs;
		} else if (table.equalsIgnoreCase("REC_TN")) {
			url = TN_URLs;
		} else if (table.equalsIgnoreCase("TVR_CALL_RECORDING")) {
			url = HO_CALL_URLs;
		} else {
			System.out.println("NO TABLE FOUND");
		}
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("TID", tid);
		map.add("SID", sid);
		map.add("STATUS", call_status);
		map.add("Call_URL", Call_url);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> restTemplate = new RestTemplate().exchange(url, HttpMethod.POST, entity, String.class);
		return restTemplate;
	}

//	public Map<String, String> urlDataBySid(String sid) throws ParseException, InterruptedException {
//		logger.info("....SID API starts here...." + sid);
//		String SID_URL = Exotel_sid_URL + sid;
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//		headers.set("Authorization", Exotel_auth_token);
//		HttpEntity<String> entity = new HttpEntity<String>(headers);
//		ResponseEntity<String> restTemplate = new RestTemplate().exchange(SID_URL, HttpMethod.GET, entity,
//				String.class);
//		Map<String, String> respData = getCallDataBySid(restTemplate.getBody());
//		String call_url = respData.get("Call_URL");
//		String call_status = respData.get("Call_Status");
//		Map<String, String> sidCallData = new HashMap<String, String>();
//		sidCallData.put("call_url", call_url);
//		sidCallData.put("call_status", call_status);
//		logger.info("....SID API ends here....");
//		return sidCallData;
//	}

	
}

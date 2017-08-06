package model;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import check.formHelper;
import check.formHelper.formdef;
import database.DBHelper;
import database.db;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;
import string.StringHelper;

@SuppressWarnings("unchecked")
public class WebModel {
	private DBHelper dbweb;
	private formHelper _form;
	private JSONObject _obj = new JSONObject();
	// private privilige privil = new
	// privilige(execRequest.getChannelValue("GrapeSID").toString());

	public WebModel() {
		dbweb = new DBHelper(appsProxy.configValue().get("db").toString(), "websiteList", "_id");
		_form = dbweb.getChecker();
		// _form.putRule("host", formdef.notNull);
		// _form.putRule("logo", formdef.notNull);
		// _form.putRule("icp", formdef.notNull);
		_form.putRule("title", formdef.notNull);
	}

	public db bind() {
		return dbweb.bind(String.valueOf(appsProxy.appid()));
	}

	/**
	 * 
	 * @param webInfo
	 * @return 1:必填数据没有填 2：ICP备案号格式错误 3:ICP已存在 4: 公安网备案号格式错误 5：title已存在
	 *         6：网站描述字数超过限制
	 */
	public String addweb(JSONObject webInfo) {
		JSONObject object = null;
		JSONObject obj;
		String info = resultMessage(99);
		if (webInfo != null) {
			info = CheckParam(webInfo);
			obj = JSONHelper.string2json(info);
			if (obj == null) {
				object = findbyid(info); // 获取新增的网站信息
				// 判断该网站是否为根网站,并填充栏目数据
				IsRoot(info, object);
				return resultMessage(object);
			}
		}
		return info;
	}

	private long AddColumn(String wbid, JSONArray array) {
		long rs = 0;
		JSONObject json;
		JSONObject temp;
		JSONObject mapMap = new JSONObject();// 栏目老id,栏目新id
		String newCID;
		String oldCID;
		JSONObject cacheObj = new JSONObject();
		for (Object obj : array) {
			json = (JSONObject) obj;// GroupInsert
			oldCID = ((JSONObject) json.get("_id")).get("$oid").toString();
			json.put("wbid", wbid);
			json.remove("_id");
			temp = JSONObject
					.toJSON(appsProxy
							.proxyCall("/GrapeContent/ContentGroup/GroupInsert/" + columnInfo(json), null, null)
							.toString());
			if (temp != null && temp.getLong("errorcode") == 0) {// 插入新栏目成功
				temp = ((JSONObject) ((JSONObject) temp.get("message")).get("records"));
				newCID = ((JSONObject) temp.get("_id")).getString("$oid");
				mapMap.put(oldCID, newCID);// 建立新老栏目ID映射表
				cacheObj.put(newCID, temp);
			} else {
				return 0;
			}
		}
		String tempFatherID;
		String fatherNewID;
		JSONObject result;
		boolean reTry = true;
		long tryNo = 0;
		long tryNax = 5;
		for (Object obj : cacheObj.keySet()) {
			temp = (JSONObject) cacheObj.get(obj);
			tempFatherID = temp.get("fatherid").toString();
			if (tempFatherID.contains("$numberLong")) {
				tempFatherID = JSONHelper.string2json(tempFatherID).getString("$numberLong");
			}
			if (!tempFatherID.equals("0")) {
				fatherNewID = mapMap.get(tempFatherID).toString();
				while (reTry && tryNo < tryNax) {
					reTry = false;
					result = JSONObject.toJSON(appsProxy
							.proxyCall("/GrapeContent/ContentGroup/GroupEdit/" + obj.toString() + "/"
											+ (new JSONObject("fatherid", fatherNewID)).toJSONString(),
									null, null)
							.toString());
					if (result != null && result.getLong("errorcode") == 99) {
						reTry = true;
						tryNo++;
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							;
						}
					} else {
						rs++;
					}
				}
				reTry = true;
			}
		}
		return rs;
	}

	private String IsRoot(String wbid, JSONObject object) {
		long l = 0;
		try {
			String fid = object.getString("fatherid");

			if (!fid.equals("0")) {
				// 获取上一级栏目
				JSONObject obj = findbyid(fid);
				if (obj != null) {
					String prevfid = obj.getString("fatherid");
					if (!prevfid.equals("0")) {
						// 获取该fid下所有栏目
						String columns = appsProxy.proxyCall("/GrapeContent/ContentGroup/getPrevColumn/" + fid).toString();
						l = AddColumn(wbid, JSONHelper.string2array(columns));
					}
				}
			} else {
				l = 1;
			}
		} catch (Exception e) {
			nlogger.logout(e);
			l = 0;
		}
		return String.valueOf(l);
	}

	private String columnInfo(JSONObject object) {
		String value = "";
		for (Object object2 : object.keySet()) {
			value = object.get(object2.toString()).toString();
			if (value.contains("$numberLong")) {
				object.put(object2.toString(), object.getInt(object2));
			}
		}
		return object.toString();
	}

	public int delete(String webid) {
		int code = 99;
		try {
			JSONObject object = bind().findOne().eq("_id", new ObjectId(webid)).delete();
			code = (object != null ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int update(String wbid, JSONObject webinfo) {
		int code = 99;
		if (webinfo != null) {
			try {
				/*
				 * if (webinfo.containsKey("icp")) { String ICP =
				 * webinfo.get("icp").toString(); if (!check_icp(ICP)) { return
				 * 2; } }
				 */
				if (webinfo.containsKey("thumbnail")) {
					String image = webinfo.getString("thumbnail");
					if (!image.equals("") && image != null) {
						image = codec.DecodeHtmlTag(image);
						image = getImageUri(codec.DecodeHtmlTag(image));
						if (image != null) {
							webinfo.put("thumbnail", image);
						}
					}
				}
				JSONObject object = bind().eq("_id", new ObjectId(wbid)).data(webinfo).update();
				code = (object != null ? 0 : 99);
			} catch (Exception e) {
				code = 99;
			}
		}
		return code;
	}

	private String getImageUri(String imageURL) {
		int i = 0;
		if (imageURL.contains("File//upload")) {
			i = imageURL.toLowerCase().indexOf("file//upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File\\upload")) {
			i = imageURL.toLowerCase().indexOf("file\\upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File/upload")) {
			i = imageURL.toLowerCase().indexOf("file/upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		return imageURL;
	}

	public int updatebywbgid(String wbgid, JSONObject webinfo) {
		int code = 99;
		if (webinfo != null) {
			try {
				long codes = bind().eq("wbgid", wbgid).data(webinfo).updateAll();
				code = (codes != 0 ? 0 : 99);
			} catch (Exception e) {
				code = 99;
			}
		}
		return code;
	}

	public String select(String webinfo) {
		JSONArray array = null;
		JSONObject object = JSONHelper.string2json(webinfo);
		if (object != null) {
			try {
				array = new JSONArray();
				for (Object object2 : object.keySet()) {
					if (object2.equals("_id")) {
						bind().eq("_id", new ObjectId(object.get(object2.toString()).toString()));
					}
					bind().eq(object2.toString(), object.get(object2.toString()));
				}
				array = bind().limit(20).select();
			} catch (Exception e) {
				array = null;
			}
		}
		return resultMessage(getImage(array));
	}

	private JSONArray getImage(JSONArray array) {
		String url = "http://" + getFile(1);
		int l = array.size();
		JSONObject obj;
		for (int i = 0; i < l; i++) {
			obj = (JSONObject) array.get(i);
			if (obj != null && obj.size() != 0 && obj.containsKey("thumbnail")) {
				url = url + obj.getString("thumbnail");
				obj.put("thumbnail", url);
				array.set(i, obj);
			}
		}
		return array;
	}

	public String selectbyid(String wbid) {
		if (wbid.contains(",")) {
			bind().or();
			String[] wbids = wbid.split(",");
			for (int i = 0, len = wbids.length; i < len; i++) {
				bind().eq("_id", new ObjectId(wbids[i]));
			}
		} else {
			bind().eq("_id", new ObjectId(wbid));
		}
		JSONArray array = bind().limit(10).select();
		return resultMessage(array);
	}

	private JSONObject findbyid(String wbid) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", new ObjectId(wbid)).find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return object;
	}

	public String selectbyWbgid(String wbgid) {
		JSONArray array = null;
		try {
			array = new JSONArray();
			array = bind().eq("wbgid", wbgid).limit(20).select();
		} catch (Exception e) {
			array = null;
		}
		return resultMessage(array);
	}

	/**
	 * 显示网站信息，只显示_id,title,wbgid,fatherid 字段
	 * 
	 * @project GrapeWebInfo
	 * @package model
	 * @file WebModel.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @param webinfo
	 * @return
	 *
	 */
	public String page(int idx, int pageSize, String webinfo) {
		long total = 0, totalSize = 0;
		JSONArray array = null;
		if (idx > 0 && pageSize > 0) {
			db db = getPageDB(webinfo);
			array = db.dirty().field("_id,title,wbgid,fatherid").page(idx, pageSize);
			totalSize = db.dirty().pageMax(pageSize);
			total = db.count();
			db.clear();
		}
		return PageShow(array, totalSize, idx, pageSize, total);
	}

	/**
	 * 显示网站信息，显示全字段
	 * 
	 * @project GrapeWebInfo
	 * @package model
	 * @file WebModel.java
	 * 
	 * @param idx
	 * @param pageSize
	 * @param webinfo
	 * @return
	 *
	 */
	public String pages(int idx, int pageSize, String webinfo) {
		long total = 0, totalSize = 0;
		JSONArray array = null;
		if (idx > 0 && pageSize > 0) {
			db db = getPageDB(webinfo);
			array = db.dirty().page(idx, pageSize);
			totalSize = db.dirty().pageMax(pageSize);
			total = db.count();
			db.clear();
		}
		return PageShow(array, totalSize, idx, pageSize, total);
	}

	private db getPageDB(String webinfo) {
		String key;
		Object value;
		db db = bind();
		if (webinfo != null) {
			JSONObject obj = JSONHelper.string2json(webinfo);
			if (obj != null && obj.size() != 0) {
				for (Object object2 : obj.keySet()) {
					key = object2.toString();
					value = obj.get(key);
					db.eq(key, value);
				}
			}
		}
		return db;
	}

	public int sort(String wbid, long num) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("sort", num);
		if (object != null) {
			try {
				JSONObject object2 = bind().eq("_id", new ObjectId(wbid)).data(object).update();
				code = (object2 != null ? 0 : 99);
			} catch (Exception e) {
				code = 99;
			}
		}
		return code;
	}

	public int setwbgid(String wbid, String wbgid) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("wbgid", wbgid);
		if (object != null) {
			try {
				JSONObject object2 = bind().eq("_id", new ObjectId(wbid)).data(object).update();
				code = (object2 != null ? 0 : 99);
			} catch (Exception e) {
				code = 99;
			}
		}
		return code;
	}

	public int settempid(String wbid, String tempid) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("tid", tempid);
		if (object != null) {
			try {
				JSONObject object2 = bind().eq("_id", new ObjectId(wbid)).data(object).update();
				code = (object2 != null ? 0 : 99);
			} catch (Exception e) {
				code = 99;
			}
		}
		return code;
	}

	// 切换网站，替换session会话中的currentWeb值
	public String WebSwitch(String wbid) {
		String sid = "";
		session session = new session();
		JSONObject obj = null;
		Object object = (String) execRequest.getChannelValue("sid");
		if (object != null) {
			try {
				obj = (JSONObject) session.getSession(object.toString());
				if (obj != null) {
					obj.put("currentWeb", wbid);
					sid = session.setget(session.get(object.toString()), obj.toString());
				}
			} catch (Exception e) {
				nlogger.logout(e);
				sid = "";
			}
		}
		return resultMessage(sid != "" ? 0 : 99, "ok");
	}

	public String setManage(String wbid, String userid) {
		int code = 99;
		// 获取该网站已存在的管理员
		JSONObject object = findbyid(wbid);
		if (object != null) {
			try {
				String ownid = (String) object.get("ownid");
				if (!("").equals(ownid)) {
					userid = String.join(",", ownid, userid);
				}
				object.put("ownid", userid);
				code = bind().eq("_id", new ObjectId(wbid)).data(object).update() != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return resultMessage(code, "设置网站管理员成功");
	}

	public int delete(String[] arr) {
		int code = 99;
		try {
			bind().or();
			for (int i = 0, len = arr.length; i < len; i++) {
				bind().eq("_id", new ObjectId(arr[i]));
			}
			long codes = bind().deleteAll();
			code = (Integer.parseInt(String.valueOf(codes)) == arr.length ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	/**
	 * 匹配icp格式
	 * 
	 * @param icp
	 *            icp格式为类似于 皖icp备11016779号 或 京ICP备05087018号2
	 * @return
	 */
	/*
	 * public boolean check_icp(String icp) { return check.check_icp(icp); }
	 */

	public boolean check_desp(String desp) {
		return desp.length() <= 1024;
	}

	public JSONObject findWebByTitle(String title) {
		JSONObject rs = bind().eq("title", title).find();
		return rs;
	}

	public JSONObject findWebByICP(String icp) {
		JSONObject rs = bind().eq("icp", icp).find();
		return rs;
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (object != null) {
			if (map.entrySet() != null) {
				Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
					if (!object.containsKey(entry.getKey())) {
						object.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		return object;
	}

	private String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	// 获取应用url[内网url或者外网url]，0表示内网，1表示外网
	public String getHost(int signal) {
		String host = null;
		try {
			if (signal == 0 || signal == 1) {
				host = getAppIp("host").split("/")[signal];
			}
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	public String getFile(int signal) {
		String host = null;
		try {
			if (signal == 0 || signal == 1) {
				host = getAppIp("file").split("/")[signal];
			}
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	private String CheckParam(JSONObject webInfo) {
		String info = "";
		if (webInfo != null) {
			try {
				if (!_form.checkRuleEx(webInfo)) {
					return resultMessage(1);
				}
				if (webInfo.containsKey("icp")) {
					String ICP = webInfo.get("icp").toString();
					if (!ICP.equals("")) {
						/*
						 * if (!check_icp(ICP)) { return resultMessage(2); }
						 */
						if (findWebByICP(ICP) != null) {
							return resultMessage(3);
						}
					}
				}
				/*
				 * if (webInfo.containsKey("policeid")) { String policeid =
				 * webInfo.get("policeid").toString(); if (!policeid.equals(""))
				 * { if (!check.CheckIcpNum(policeid)) { return
				 * resultMessage(4); } } }
				 */
				if (webInfo.containsKey("thumbnail")) {
					String image = webInfo.getString("thumbnail");
					if (!image.equals("") && image != null) {
						image = codec.DecodeHtmlTag(image);
						image = getImageUri(codec.DecodeHtmlTag(image));
						if (image != null) {
							webInfo.put("thumbnail", image);
						}
					}
				}
				String webname = webInfo.get("title").toString();
				if (findWebByTitle(webname) != null) {
					return resultMessage(5);
				}
				if (webInfo.containsKey("desp")) {
					if (!check_desp(webInfo.get("desp").toString())) {
						return resultMessage(6);
					}
				}
				info = bind().data(webInfo).insertOnce().toString();
			} catch (Exception e) {
				nlogger.logout(e);
				return resultMessage(99);
			}
		}
		return info;
	}

	/**
	 * 以某网站为根节点，获得其自身和下级全部网站id,输出成 网站id,网站id,网站id...
	 * 
	 * @project GrapeSuggest
	 * @package interfaceApplication
	 * @file Suggest.java
	 * 
	 * @param root
	 * @return
	 * 
	 */
	public String getWebID4All(String root) {
		db db = bind();
		JSONArray data = db.eq("fatherid", root).select();
		JSONObject object;
		String tmpWbid;
		String rsString = root;
		for (Object obj : data) {
			object = (JSONObject) obj;
			tmpWbid = ((JSONObject) object.get("_id")).get("$oid").toString();
			rsString = rsString + "," + getWebID4All(tmpWbid);
		}
		return StringHelper.fixString(rsString, ',');
	}

	/**
	 * 分页显示数据输出格式
	 * 
	 * @project GrapeWebInfo
	 * @package model
	 * @file WebModel.java
	 * 
	 * @param array
	 *            满足条件的数据
	 * @param totalSize
	 *            满足条件数据 总页数
	 * @param currentPage
	 *            当前页
	 * @param pageSize
	 *            每页数据量
	 * @param total
	 *            总数据量
	 * @return
	 *
	 */
	public String PageShow(JSONArray array, long totalSize, int currentPage, int pageSize, long total) {
		JSONObject object = new JSONObject();
		object.put("total", total);
		object.put("totalSize", totalSize);
		object.put("currentPage", currentPage);
		object.put("pageSize", pageSize);
		object.put("data", (array == null) ? new JSONArray() : array);
		return resultMessage(object);
	}

	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	public String resultMessage(int num) {
		return resultMessage(num, "");
	}

	private String resultMessage(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		_obj.put("records", array);
		return resultMessage(0, _obj.toString());
	}

	public String resultMessage(int num, String msg) {
		String message = "";
		switch (num) {
		case 0:
			message = msg;
			break;
		case 1:
			message = "必填数据没有填";
			break;
		case 2:
			message = "ICP备案号格式错误";
			break;
		case 3:
			message = "ICP备案号已存在";
			break;
		case 4:
			message = "公安网备案号格式错误";
			break;
		case 5:
			message = "网站名称不允许重复";
			break;
		case 6:
			message = "字数超过限制";
			break;
		case 7:
			message = "新增网站成功，拉取栏目信息失败";
			break;
		case 8:
			message = "该网站所属根网站无固定栏目";
			break;
		default:
			message = "其他异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, message);
	}
}

package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import check.formHelper;
import check.formHelper.formdef;
import database.DBHelper;
import database.db;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import nlogger.nlogger;

@SuppressWarnings("unchecked")
public class WebGroupModel {
	private GrapeTreeDBModel dbwebgroup;
    private GrapeDBSpecField gDbSpecField;
	private JSONObject _obj = new JSONObject();

	public WebGroupModel() {
		dbwebgroup = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("WebGroup"));
        dbwebgroup.descriptionModel(gDbSpecField);
        dbwebgroup.bindApp();
	}

	/**
	 * 新增站群
	 * 
	 * @param webgroupInfo
	 * @return 0：添加数据成功 1：存在非空字段 2：存在同名站群 其它异常
	 */
	public int add(JSONObject webgroupInfo) {
		int code = 99;
		if (webgroupInfo != null) {
			try {
				// 判断库中是否存在同名站群
				String name = webgroupInfo.get("name").toString();
				if (findByName(name) != null) {
					return 2;
				}
				code = dbwebgroup.data(webgroupInfo).autoComplete().insertOnce() != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int delete(String id) {
		int code = 99;
		try {
			JSONObject object = dbwebgroup.eq("_id", new ObjectId(id)).delete();
			code = (object != null ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public String search() {
		JSONArray array = null;
		try {
			array = new JSONArray();
			array = dbwebgroup.limit(30).select();
		} catch (Exception e) {
			nlogger.logout(e);
			array = null;
		}
		return resulmessage(array);
	}

	public String select(String webinfo) {
		JSONObject object = JSONHelper.string2json(webinfo);
		JSONArray array = null;
		try {
			array = new JSONArray();
			for (Object object2 : object.keySet()) {
				if (object2.equals("_id")) {
					dbwebgroup.eq("_id", new ObjectId(object.get(object2.toString()).toString()));
				}
				dbwebgroup.eq(object2.toString(), object.get(object2.toString()));
			}
			array = dbwebgroup.limit(30).select();
		} catch (Exception e) {
			nlogger.logout(e);
			array = null;
		}
		return resulmessage(array);
	}

	public JSONObject find(String wbid) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = dbwebgroup.eq("_id", new ObjectId(wbid)).find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return object;
	}

	public int update(String wbgid, String webinfo) {
		int code = 99;
		JSONObject _webinfo = JSONHelper.string2json(webinfo);
		if (_webinfo != null) {
			if (_webinfo.containsKey("name")) {
				String name = _webinfo.get("name").toString();
				if (findByName(name) != null) {
					return 2;
				}
			}
			JSONObject object = dbwebgroup.eq("_id", new ObjectId(wbgid)).data(_webinfo).update();
			code = (object != null ? 0 : 99);
		}
		return code;
	}

	public int sortAndFatherid(JSONObject object) {
		int code = 99;
		if (object!=null) {
			JSONObject _obj = new JSONObject();
			if (object.containsKey("sort")) {
				_obj.put("sort", object.get("sort").toString());
			}
			if (object.containsKey("fatherid")) {
				_obj.put("fatherid", object.get("fatherid").toString());
			}
			 JSONObject objs = dbwebgroup.eq("_id", new ObjectId(object.get("_id").toString())).data(_obj).update();
			 code = (objs!= null ? 0 : 99);
		}
		return code;
	}

	public String page(int idx, int pageSize) {
		JSONObject object = null;
		try {
			JSONArray array = dbwebgroup.page(idx, pageSize);
			object = new JSONObject();
			object.put("totalSize", (int) Math.ceil((double) dbwebgroup.count() / pageSize));
			object.put("currentPage", idx);
			object.put("pageSize", pageSize);
			object.put("data", array);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resulmessage(object);
	}

	public String page(String webinfo, int idx, int pageSize) {
		JSONObject object = null;
		JSONObject obj = JSONHelper.string2json(webinfo);
		if (obj!=null) {
			try {
				for (Object object2 : obj.keySet()) {
					dbwebgroup.eq(object2.toString(), JSONHelper.string2json(webinfo).get(object2.toString()));
				}
				JSONArray array = dbwebgroup.page(idx, pageSize);
				object = new JSONObject();
				object.put("totalSize", (int) Math.ceil((double) dbwebgroup.count() / pageSize));
				object.put("currentPage", idx);
				object.put("pageSize", pageSize);
				object.put("data", array);
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			}
		}
		return resulmessage(object);
	}

	public JSONObject findByName(String name) {
		return dbwebgroup.eq("name", name).find();
	}

	public String findbyfatherid(String fatherid) {
		JSONArray array = dbwebgroup.eq("fatherid", fatherid).limit(30).select();
		JSONObject _obj;
		String name = null;
		if (array.size()!=0) {
			for (Object object : array) {
				_obj = (JSONObject) object;
				name = _obj.get("name").toString();
			}
		}
		return name;
	}

	// public String setfatherid(String wbgid, String fathrid) {
	// JSONObject _obj = new JSONObject();
	// _obj.put("fatherid", fathrid);
	// return dbwebgroup.eq("_id", new
	// ObjectId(wbgid)).data(_obj).update().toString();
	// }

	public int delete(String[] arr) {
		int code = 99;
		try {
			dbwebgroup.or();
			for (int i = 0, len = arr.length; i < len; i++) {
				dbwebgroup.eq("_id", new ObjectId(arr[i]));
			}
			long codes = dbwebgroup.deleteAll();
			code = (Integer.parseInt(String.valueOf(codes)) == arr.length ? 0 : 99);
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
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

	private String resulmessage(JSONObject object) {
		if (object==null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultmessage(0, _obj.toString());
	}

	private String resulmessage(JSONArray array) {
		if (array==null) {
			array = new JSONArray();
		}
		_obj.put("records", array);
		return resultmessage(0, _obj.toString());
	}

	public String resultmessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填字段没有填";
			break;
		case 2:
			msg = "已存在该站群";
			break;
		case 3:
			msg = "批量操作失败";
			break;
		default:
			msg = "其他操作异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}

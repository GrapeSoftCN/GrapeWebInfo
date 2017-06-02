package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import authority.privilige;
import database.db;
import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;
import esayhelper.formHelper.formdef;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;

@SuppressWarnings("unchecked")
public class WebModel {
	private static DBHelper dbweb;
	private static formHelper _form;
	private JSONObject _obj = new JSONObject();
	// private privilige privil = new
	// privilige(execRequest.getChannelValue("GrapeSID").toString());

	static {
		dbweb = new DBHelper(appsProxy.configValue().get("db").toString(), "websiteList", "_id");
		// dbweb = new DBHelper("mongodb", "websiteList", "_id");
		_form = dbweb.getChecker();
	}

	public WebModel() {
		_form.putRule("host", formdef.notNull);
		_form.putRule("logo", formdef.notNull);
		_form.putRule("icp", formdef.notNull);
		_form.putRule("title", formdef.notNull);
	}

	private db bind() {
		return dbweb.bind(String.valueOf(appsProxy.appid()));
	}

	/**
	 * 
	 * @param webInfo
	 * @return 1:必填数据没有填 2：ICP备案号格式错误 3:ICP已存在 4: 公安网备案号格式错误 5：title已存在
	 *         6：网站描述字数超过限制
	 */
	public int addweb(JSONObject webInfo) {
		int code = 99;
		if (webInfo != null) {
			try {
				if (!_form.checkRuleEx(webInfo)) {
					return 1;
				}
				String ICP = webInfo.get("icp").toString();
				if (!check_icp(ICP)) {
					return 2;
				}
				if (findWebByICP(ICP) != null) {
					return 3;
				}
				if (webInfo.containsKey("policeid")) {
					String policeid = webInfo.get("policeid").toString();
					if (!policeid.equals("")) {
						if (!Check.CheckIcpNum(policeid)) {
							return 4;
						}
					}
				}
				String webname = webInfo.get("title").toString();
				if (findWebByTitle(webname) != null) {
					return 5;
				}
				if (webInfo.containsKey("desp")) {
					if (!check_desp(webInfo.get("desp").toString())) {
						return 6;
					}
				}
				Object object = bind().data(webInfo).insertOnce();
				code = (object != null ? 0 : 99);
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
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
				if (webinfo.containsKey("icp")) {
					String ICP = webinfo.get("icp").toString();
					if (!check_icp(ICP)) {
						return 2;
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
		return resultMessage(array);
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
			object = bind().eq("_id", new ObjectId(wbid)).field("ownid").find();
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

	public String page(int idx, int pageSize) {
		JSONObject object = null;
		try {
			JSONArray array = bind().page(idx, pageSize);
			object = new JSONObject();
			object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
			object.put("currentPage", idx);
			object.put("pageSize", pageSize);
			object.put("data", array);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	public String page(String webinfo, int idx, int pageSize) {
		JSONObject object = null;
		JSONObject obj = JSONHelper.string2json(webinfo);
		if (obj != null) {
			try {
				for (Object object2 : obj.keySet()) {
					bind().eq(object2.toString(), JSONHelper.string2json(webinfo).get(object2.toString()));
				}
				JSONArray array = bind().page(idx, pageSize);
				object = new JSONObject();
				object.put("totalSize", (int) Math.ceil((double) bind().count() / pageSize));
				object.put("currentPage", idx);
				object.put("pageSize", pageSize);
				object.put("data", array);
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			}
		}
		return resultMessage(object);
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
				if (obj!=null) {
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
	public boolean check_icp(String icp) {
		return Check.check_icp(icp);
	}

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
	 * 生成32位随机编码
	 * 
	 * @return
	 */
	public static String getID() {
		String str = UUID.randomUUID().toString().trim();
		return str.replace("-", "");
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

	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
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
		default:
			message = "其他异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, message);
	}
}

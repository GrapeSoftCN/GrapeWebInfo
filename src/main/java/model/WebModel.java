package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;
import esayhelper.formHelper.formdef;

@SuppressWarnings("unchecked")
public class WebModel {
	private static DBHelper dbweb;
	private static formHelper _form;
	private JSONObject _obj = new JSONObject();

	static {
		dbweb = new DBHelper(appsProxy.configValue().get("db").toString(),
				"websiteList", "_id");
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
		if (!_form.checkRuleEx(webInfo)) {
			return 1;
		}
		String ICP = webInfo.get("icp").toString();
		if (!check_icp(ICP)) {
			return 2;
		}
		String policeid = webInfo.get("policeid").toString();
		if (!policeid.equals("")) {
			if (!Check.CheckIcpNum(policeid)) {
				return 4;
			}
		}
		if (findWebByICP(ICP) != null) {
			return 3;
		}
		String webname = webInfo.get("title").toString();
		if (findWebByTitle(webname) != null) {
			return 5;
		}
		if (!check_desp(webInfo.get("desp").toString())) {
			return 6;
		}
		return bind().data(webInfo).insertOnce() != null ? 0 : 99;
	}

	public int delete(String webid) {
		return bind().findOne().eq("_id", new ObjectId(webid)).delete() != null
				? 0 : 99;
	}

	public int update(String wbid, JSONObject webinfo) {
		String ICP = webinfo.get("icp").toString();
		if (webinfo.containsKey("icp")) {
			if (!check_icp(ICP)) {
				return 2;
			}
		}
		return bind().eq("_id", new ObjectId(wbid)).data(webinfo)
				.update() != null ? 0 : 99;
	}

	public int updatebywbgid(String wbgid, JSONObject webinfo) {
		return bind().eq("wbgid", wbgid).data(webinfo).updateAll() != 0 ? 0 : 99;
	}

	public String select(String webinfo) {
		JSONObject object = JSONHelper.string2json(webinfo);
		Set<Object> set = object.keySet();
		for (Object object2 : set) {
			if (object2.equals("_id")) {
				bind().eq("_id", new ObjectId(
						object.get(object2.toString()).toString()));
			}
			bind().eq(object2.toString(), object.get(object2.toString()));
		}
		JSONArray array = bind().limit(20).select();
		return resultMessage(array);
	}

	public String selectbyid(String wbid) {
		if (wbid.contains(",")) {
			dbweb = (DBHelper) bind().or();
			String[] wbids = wbid.split(",");
			for (int i = 0, len = wbids.length; i < len; i++) {
				bind().eq("_id", new ObjectId(wbids[i]));
			}
		} else {
			dbweb = (DBHelper) bind().eq("_id", new ObjectId(wbid));
		}
		JSONArray array = bind().limit(10).select();
		return resultMessage(array);
	}

	public String selectbyWbgid(String wbgid) {
		JSONArray array = bind().limit(20).select();
		return resultMessage(array);
	}

	public String page(int idx, int pageSize) {
		JSONArray array = bind().page(idx, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) bind().count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return resultMessage(object);
	}

	public String page(String webinfo, int idx, int pageSize) {
		Set<Object> set = JSONHelper.string2json(webinfo).keySet();
		for (Object object2 : set) {
			bind().eq(object2.toString(),
					JSONHelper.string2json(webinfo).get(object2.toString()));
		}
		JSONArray array = bind().page(idx, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) bind().count() / pageSize));
		object.put("currentPage", idx);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return resultMessage(object);
	}

	public int sort(String wbid, long num) {
		JSONObject object = new JSONObject();
		object.put("sort", num);
		return bind().eq("_id", new ObjectId(wbid)).data(object).update() != null
				? 0 : 99;
	}

	public int setwbgid(String wbid, String wbgid) {
		JSONObject object = new JSONObject();
		object.put("wbgid", wbgid);
		return bind().eq("_id", new ObjectId(wbid)).data(object).update() != null
				? 0 : 99;
	}

	public int settempid(String wbid, String tempid) {
		JSONObject object = new JSONObject();
		object.put("tid", tempid);
		return bind().eq("_id", new ObjectId(wbid)).data(object).update() != null
				? 0 : 99;
	}

	public int delete(String[] arr) {
		bind().or();
		for (int i = 0; i < arr.length; i++) {
			bind().eq("_id", arr[i]);
		}
		return bind().deleteAll() == arr.length ? 0 : 3;
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
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator
						.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	private String resultMessage(JSONObject object) {
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	private String resultMessage(JSONArray array) {
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

package interfaceApplication;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import apps.appsProxy;
import esayhelper.JSONHelper;
import model.WebGroupModel;

/**
 * 站群 备注：涉及到的id，都是数据表中的_id 返回数据类型为{"message":显示数据或操作提示,"errorcode":错误码}
 * 错误码为0，表示数据或操作正常
 *
 */
public class WebGroup {
	private WebGroupModel webgroup = new WebGroupModel();
	private HashMap<String, Object> defmap = new HashMap<>();

	public WebGroup() {
		defmap.put("ownid", 1);
		defmap.put("sort", 0);
		defmap.put("fatherid", 0); // 默认fatherid为0，为一级站群
		defmap.put("rPlv", 1000); // 读取 权限值
		defmap.put("uPlv", 2000); // 修改 权限值
		defmap.put("dPlv", 3000); // 删除 权限值
	}

	public String WebGroupInsert(String webgroupInfo) {
		JSONObject object = webgroup.AddMap(defmap, JSONHelper.string2json(webgroupInfo));
		return webgroup.resultmessage(webgroup.add(object), "站群新增成功");
	}

	public String WebGroupDelete(String id) {
		int code = 0;
		try {
			// 判断该站群下，是否存在网站
			String message = appsProxy
					.proxyCall(callhost(), appsProxy.appid() + "/17/WebInfo/WebFindByWbId/s:" + id, null, "").toString();
			if (JSONHelper.string2json(message)!=null) {
				String records = JSONHelper.string2json(message).get("message").toString();
				JSONArray array = JSONHelper.string2array(JSONHelper.string2json(records).get("records").toString());
				if (array.size() != 0) {
					message = appsProxy
							.proxyCall(callhost(), appsProxy.appid() + "/17/WebInfo/WebUpd/s:" + id, null,"").toString();
					if (JSONHelper.string2json(message)!=null) {
						long num = (long) JSONHelper.string2json(message).get("errorcode");
						code = Integer.parseInt(String.valueOf(num));
					}
				}
			}
			if (code == 0) {
				code = webgroup.delete(id);
			}
		} catch (Exception e) {
			code = 99;
		}
		return webgroup.resultmessage(code, "站群删除成功");
	}

	public String WebGroupSelect() {
		return webgroup.search();
	}

	public String WebGroupFind(String webinfo) {
		return webgroup.select(webinfo);
	}

	public String WebGroupFindBywbid(String wbid) {
		return webgroup.find(wbid).toString();
	}

	public String WebGroupUpdate(String wbgid, String webgroupInfo) {
		return webgroup.resultmessage(webgroup.update(wbgid, webgroupInfo), "站群修改成功");
	}

	public String WebGroupPage(int idx, int pageSize) {
		return webgroup.page(idx, pageSize);
	}

	public String WebGroupPageBy(int idx, int pageSize, String webinfo) {
		return webgroup.page(webinfo, idx, pageSize);
	}

	public String WebGroupUpBatch(String arraystring) {
		int code = 0;
		JSONArray array = (JSONArray) JSONValue.parse(arraystring);
		for (int i = 0; i < array.size(); i++) {
			if (code != 0) {
				return webgroup.resultmessage(3, "");
			}
			JSONObject object = (JSONObject) array.get(i);
			code = webgroup.sortAndFatherid(object);
		}
		return webgroup.resultmessage(code, "设置排序值或层级成功");
	}

	/**
	 * 修改排序值
	 * 
	 * @param webinfo包括{"_id":"value","sort":x}
	 * @return
	 */
	public String WebGroupSort(String webinfo) {
		return webgroup.resultmessage(webgroup.sortAndFatherid(JSONHelper.string2json(webinfo)), "设置排序值成功");
	}

	/**
	 * 设置上级站群，默认fatherid为0时，是一级站群
	 * 
	 * @param webinfo包括{"_id":"value","fatherid":x}
	 * @return
	 */
	public String WebGroupSetfatherid(String webinfo) {
		return webgroup.resultmessage(webgroup.sortAndFatherid(JSONHelper.string2json(webinfo)), "设置上级站群成功");
	}

	public String WebGroupBatchDelete(String arrys) {
		return webgroup.resultmessage(webgroup.delete(arrys.split(",")), "批量删除成功");
	}

	private String callhost() {
		return getAppIp("host").split("/")[0];
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
}

package interfaceApplication;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import database.db;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import model.WebModel;

/**
 * 网站信息 备注：涉及到的id都是数据表中的_id
 *
 */
@SuppressWarnings("unchecked")
public class WebInfo {
	private WebModel web = new WebModel();
	/**
	 * 
	 * @param webInfo
	 *            （必填字段："host", "logo", "icp", "title"）
	 * @return 1:必填数据没有填 2：ICP备案号格式错误 3:ICP已存在 4: 公安网备案号格式错误 5：title已存在
	 *         6：网站描述字数超过限制
	 */
	public String WebInsert(String webInfo) {
		JSONObject object = JSONObject.toJSON(webInfo);
//		JSONObject object = web.AddMap(map, JSONHelper.string2json(webInfo));
		return web.addweb(object);
	}

	/**
	 * 
	 * @param wbid
	 *            _id对应的值
	 * @return
	 */
	public String WebDelete(String wbid) {
		return web.resultMessage(web.delete(wbid), "删除网站信息成功");
	}

	public String WebUpdate(String wbid, String WebInfo) {
		return web.resultMessage(web.update(wbid, JSONHelper.string2json(WebInfo)), "网站信息更新成功");
	}

	// 通过站群id更新网站信息
	public String WebUpd(String wbid) {
		JSONObject webinfo = new JSONObject();
		webinfo.put("wbgid", 0);
		return web.resultMessage(web.updatebywbgid(wbid, webinfo), "网站信息更新成功");
	}

	public String Webfind(String wbinfo) {
		return web.select(wbinfo);
	}

	// 显示_id,title,wbgid,fatherid
	public String WebPage(int idx, int pageSize) {
		return web.page(idx, pageSize, null);
	}

	// 显示_id,title,wbgid,fatherid
	public String WebPageBy(int idx, int pageSize, String webinfo) {
		return web.page(idx, pageSize, webinfo);
	}

	// 显示所有字段
	public String WebPageBack(int idx, int pageSize) {
		return web.pages(idx, pageSize, null);
	}

	// 显示所有字段
	public String WebPageByBack(int idx, int pageSize, String webinfo) {
		return web.pages(idx, pageSize, webinfo);
	}

	public String WebSort(String wbid, int num) {
		return web.resultMessage(web.sort(wbid, num), "排序值设置成功");
	}

	public String WebSetwbg(String wbid, String wbgid) {
		return web.resultMessage(web.setwbgid(wbid, wbgid), "站点设置成功");
	}

	public String setTemp(String wbid, String tempid) {
		return web.resultMessage(web.settempid(wbid, tempid), "设置模版成功");
	}

	public String WebBatchDelete(String wbid) {
		return web.resultMessage(web.delete(wbid.split(",")), "批量删除成功");
	}

	public String WebFindById(String wbid) {
		return web.selectbyid(wbid);
	}

	public String WebFindByWbId(String wbgid) {
		return web.selectbyWbgid(wbgid);
	}

	// 设置网站管理员
	// public String setManager(String wbid, String userid) {
	// int roleplv = 0;
	// String info = web.resultMessage(99, "");
	// String sid = (String) execRequest.getChannelValue("GrapeSID");
	// if (sid != null) {
	// privilige pril = new privilige(sid);
	// roleplv = pril.getRolePV(appsProxy.appidString());
	// }
	// if (roleplv > 10000) {
	// // 设置管理员
	// info = web.setManage(wbid, userid);
	// }
	// return info;
	// }

	// 切换网站
	public String SwitchWeb(String wbid) {
		return web.WebSwitch(wbid);
	}

	// 获得当前网站节点树，包含自身及下级全部网站
	public String getWebTree(String root) {
		return web.getWebID4All(root);
	}

	// 获取当前站点及全部父站点信息
	public String getFatherWeb(int idx, int pageSize, String root) {
		// String wbid = web.getWebFID(root); //包含自身网站
		String wbid = web.getFID(root); // 不包含自身网站
		return getWeb(wbid, idx, pageSize);
	}
	// 批量获取网站信息
		private String getWeb(String wbid, int idx, int pageSize) {
			long total = 0, totalSize = 0;
			JSONArray array =null;
			if (!wbid.equals("")) {
				String[] wbids = wbid.split(",");
				GrapeTreeDBModel db = web.getDB();
				db.or();
				for (String value : wbids) {
					if (value!=null && !value.equals("")) {
						db.eq("_id", value);
					}
				}
				array = db.dirty().page(idx, pageSize);
				totalSize = db.dirty().pageMax(pageSize);
				total = db.dirty().count();
			}
			return web.PageShow(array, totalSize, idx, pageSize, total);
		}

	// 批量获取网站信息
	public String getAllWeb(int idx, int pageSize, String root) {
		long total = 0, totalSize = 0;
		String wbid = getWebTree(root);
		String[] wbids = wbid.split(",");
		GrapeTreeDBModel db = web.getDB();
		db.or();
		for (String value : wbids) {
			db.eq("_id", value);
		}
		JSONArray array = db.dirty().page(idx, pageSize);
		totalSize = db.dirty().pageMax(pageSize);
		total = db.dirty().count();
		return web.PageShow(array, totalSize, idx, pageSize, total);
	}

	// 获取网站默认缩略图
	public String getImage(String wbid) {
		String url = "http://" + web.getFile(1);
		String image = "";
		GrapeTreeDBModel db = web.getDB();
		JSONObject obj = db.eq("_id", wbid).field("thumbnail").limit(1).find();
		if (obj != null && obj.size() != 0 && obj.containsKey("thumbnail")) {
			image = obj.getString("thumbnail");
			image = url + image;
		}
		obj.put("thumbnail", image);
		return obj.toJSONString();
	}

	/**
	 * 查询网站访问量
	 * 
	 * @project GrapeWebInfo
	 * @package interfaceApplication
	 * @file WebInfo.java
	 * 
	 * @param condString
	 * @return
	 *
	 */
	public String view(String condString) {
		JSONObject object = null;
		String temp = "";
		JSONArray condArray = JSONArray.toJSONArray(condString);
		if (condArray != null && condArray.size() != 0) {
			GrapeTreeDBModel db = web.getDB();
			object = db.where(condArray).field("allno").find();
		}
		if (object != null && object.size() != 0) {
			temp = object.getString("allno");
		}
		if (temp.contains("$numberLong")) {
			temp = JSONObject.toJSON(temp).getString("$numberLong");
		}
		object.put("allno", temp);
		return web.resultMessage(object);
	}

	/**
	 * 网站访问量增加，即allno+1
	 * 
	 * @project GrapeWebInfo
	 * @package interfaceApplication
	 * @file WebInfo.java
	 * 
	 * @param condString
	 * @return
	 *
	 */
	public String viewCount(String condString) {
		int code = 99;
		String data = "{\"allno\":0}";
		JSONArray condArray = JSONArray.toJSONArray(condString);
		if (condArray != null && condArray.size() != 0) {
			int count = getCount(condArray);
			data = "{\"allno\":" + count + "}";
			GrapeTreeDBModel db = web.getDB();
			code = db.where(condArray).data(data).update() != null ? 0 : 99;
		}
		return web.resultMessage(code, "新增访问量");
	}

	// 当前网站的统计量 +1
	private int getCount(JSONArray condArray) {
		int count = 0;
		GrapeTreeDBModel db = web.getDB();
		JSONObject object = db.where(condArray).field("allno").limit(1).find();
		if (object != null && object.size() != 0) {
			if (object.containsKey("allno")) {
				String counts = object.getString("allno");
				if (counts.contains("$numberLong")) {
					counts = JSONObject.toJSON(counts).getString("$numberLong");
				}
				count = Integer.parseInt(counts);
			}
		}
		return count + 1;
	}
}

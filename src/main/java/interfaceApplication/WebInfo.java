package interfaceApplication;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import authority.privilige;
import esayhelper.JSONHelper;
import model.WebModel;
import rpc.execRequest;
import session.session;

/**
 * 网站信息 备注：涉及到的id都是数据表中的_id
 *
 */
@SuppressWarnings("unchecked")
public class WebInfo {
	private WebModel web = new WebModel();
	private HashMap<String, Object> map = new HashMap<>();

	// private static int userPlv;
	// static{
	// userPlv = Integer.parseInt(execRequest._run("GrapeAuth/Auth/getUserPlv",
	// null).toString());
	// }
	public WebInfo() {
		map.put("ownid", 0);
		map.put("engerid", 0);
		map.put("gov", "12");
		map.put("desp", "");
		map.put("policeid", "");
		map.put("wbgid", 0);
		map.put("isdelete", 0);
		map.put("isvisble", 0);
		map.put("tid", 0);
		map.put("sort", 0);
		map.put("authid", 0);
		map.put("taskid", 0);
		map.put("rPlv", 1000); // 读取 权限值
		map.put("uPlv", 2000); // 修改 权限值
		map.put("dPlv", 3000); // 删除 权限值
	}

	/**
	 * 
	 * @param webInfo
	 *            （必填字段："host", "logo", "icp", "title"）
	 * @return 1:必填数据没有填 2：ICP备案号格式错误 3:ICP已存在 4: 公安网备案号格式错误 5：title已存在
	 *         6：网站描述字数超过限制
	 */
	public String WebInsert(String webInfo) {
		// String code = execRequest._run("GrapeAuth/Auth/InsertPLV",
		// null).toString();
		// if (!"0".equals(code)) {
		// return model.resultMessage(4, "");
		// }
		JSONObject object = web.AddMap(map, JSONHelper.string2json(webInfo));
		return web.resultMessage(web.addweb(object), "新增网站信息成功");
	}

	/**
	 * 
	 * @param wbid
	 *            _id对应的值
	 * @return
	 */
	public String WebDelete(String wbid) {
		// int dplv =
		// Integer.parseInt(web.selectbyid(id).get("dPlv").toString());
		// if (userPlv<dplv) {
		// return model.resultMessage(6, "");
		// }
		return web.resultMessage(web.delete(wbid), "删除网站信息成功");
	}

	public String WebUpdate(String wbid, String WebInfo) {
		// int dplv =
		// Integer.parseInt(web.selectbyid(id).get("dPlv").toString());
		// if (userPlv<dplv) {
		// return model.resultMessage(6, "");
		// }
		return web.resultMessage(web.update(wbid, JSONHelper.string2json(WebInfo)), "网站信息更新成功");
	}

	// 通过站群id更新网站信息
	public String WebUpd(String wbid) {
		// int dplv =
		// Integer.parseInt(web.selectbyid(id).get("dPlv").toString());
		// if (userPlv<dplv) {
		// return model.resultMessage(6, "");
		// }
		JSONObject webinfo = new JSONObject();
		webinfo.put("wbgid", 0);
		return web.resultMessage(web.updatebywbgid(wbid, webinfo), "网站信息更新成功");
	}

	public String Webfind(String wbinfo) {
		return web.select(wbinfo);
	}

	public String WebPage(int idx, int pageSize) {
		return web.page(idx, pageSize);
	}

	public String WebPageBy(int idx, int pageSize, String webinfo) {
		return web.page(webinfo, idx, pageSize);
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
	public String setManager(String wbid,String userid) {
		String info = web.resultMessage(99,"");
		privilige pril = new privilige((String) execRequest.getChannelValue("GrapeSID"));
		int roleplv = pril.getRolePV();
		if (roleplv > 10000) {
			//设置管理员
			info = web.setManage(wbid, userid);
		}
		return info;
	}

	//切换网站
	public String SwitchWeb(String wbid) {
		return web.WebSwitch(wbid);
	}
}

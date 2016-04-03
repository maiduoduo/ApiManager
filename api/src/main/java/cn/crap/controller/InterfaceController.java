package cn.crap.controller;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.crap.framework.BiyaoBizException;
import cn.crap.framework.JsonResult;
import cn.crap.framework.auth.AuthPassport;
import cn.crap.framework.base.BaseController;
import cn.crap.inter.service.IErrorService;
import cn.crap.inter.service.IInterfaceService;
import cn.crap.inter.service.IModuleService;
import cn.crap.model.Error;
import cn.crap.model.Interface;
import cn.crap.model.Module;
import cn.crap.utils.Cache;
import cn.crap.utils.Const;
import cn.crap.utils.DateFormartUtil;
import cn.crap.utils.MyCookie;
import cn.crap.utils.MyString;
import cn.crap.utils.Page;
import cn.crap.utils.ParameterType;
import cn.crap.utils.Tools;

@Scope("prototype")
@Controller
@RequestMapping("/interface")
public class InterfaceController extends BaseController {

	@Autowired
	private IInterfaceService interfaceService;
	@Autowired
	private IModuleService moduleService;
	@Autowired
	private IErrorService errorService;

	@RequestMapping("/list.do")
	@ResponseBody
	@AuthPassport
	public JsonResult list(@ModelAttribute Interface interFace,
			@RequestParam(defaultValue = "1") Integer currentPage) {
		return interfaceService.getInterfaceList(page, map, interFace, currentPage);
	}


	@RequestMapping("/detail.do")
	@ResponseBody
	@AuthPassport
	public JsonResult detail(@ModelAttribute Interface interFace,
			String currentId) {
		interFace = interfaceService.get(interFace.getId());
		if (interFace == null) {
			interFace = new Interface();
			interFace.setModuleId(currentId);
			interFace.setModuleName(moduleService.get(currentId)
					.getModuleName());
		}
		return new JsonResult(1, interFace);
	}
	@RequestMapping("/webList.do")
	@ResponseBody
	public JsonResult webList(@ModelAttribute Interface interFace,
			@RequestParam(defaultValue = "1") Integer currentPage,String password,String visitCode) throws BiyaoBizException {
		Module module = moduleService.get(interFace.getModuleId());
		Tools.canVisitModule(module.getPassword(), password, visitCode, request);
		return interfaceService.getInterfaceList(page, map,interFace, currentPage);
	}

	
	@RequestMapping("/webDetail.do")
	@ResponseBody
	public JsonResult webDetail(@ModelAttribute Interface interFace,String password,String visitCode) throws BiyaoBizException {
		interFace = interfaceService.get(interFace.getId());
		Module module = moduleService.get(interFace.getModuleId());
		Tools.canVisitModule(module.getPassword(), password, visitCode, request);
		return new JsonResult(1, interFace);
	}

	@RequestMapping("/addOrUpdate.do")
	@ResponseBody
	@AuthPassport(authority=Const.AUTH_INTERFACE)
	public JsonResult addOrUpdate(
			@ModelAttribute Interface interFace) {
		if(MyString.isEmpty(interFace.getUrl()))
			return new JsonResult(new BiyaoBizException("000005"));
		interFace.setUrl(interFace.getUrl().trim());
		String errorIds = interFace.getErrorList();
		if (errorIds != null && !errorIds.equals("")) {
			map = Tools.getMap("errorCode|in", Tools.getIdsFromField(errorIds));

			Module module = moduleService.get(interFace
					.getModuleId());
			while (module != null && !module.getParentId().equals("0")) {
				module = moduleService.get(module.getParentId());
			}
			map.put("moduleId", module.getModuleId());
			List<Error> errors = errorService.findByMap(map, null,
					null);
			interFace.setErrors(JSONArray.fromObject(errors).toString());
		}else{
			interFace.setErrors("[]");
		}
		interFace.setUpdateBy("userName："+request.getSession().getAttribute(Const.SESSION_ADMIN).toString()+" | trueName："+
				request.getSession().getAttribute(Const.SESSION_ADMIN_TRUENAME).toString());
		interFace.setUpdateTime(DateFormartUtil.getDateByFormat(DateFormartUtil.YYYY_MM_DD_HH_mm));
		interFace.setRequestExam("请求地址:"+interFace.getUrl()+"\r\n");
		if(!MyString.isEmpty(interFace.getParam())){
			JSONArray json = JSONArray.fromObject(interFace.getParam());
			StringBuilder headers = new StringBuilder("请求头:\r\n");
			StringBuilder params = new StringBuilder("请求参数:\r\n");
			JSONObject obj = null;
			for(int i=0;i<json.size();i++){  
				obj = (JSONObject) json.get(i);
		        if(obj.containsKey("parameterType")&&obj.getString("parameterType").equals(ParameterType.HEADER.name())){
		        	headers.append("\t"+obj.getString("name")+"=xxxx\r\n");
		        }else{
		        	params.append("\t"+obj.getString("name")+"=xxxx\r\n");
		        }
		    }  
			interFace.setRequestExam(interFace.getRequestExam()+headers.toString()+params.toString());
			
		}
		if (!MyString.isEmpty(interFace.getId())) {
			interfaceService.update(interFace);
		} else {
			interFace.setId(null);
			Interface example = new Interface();
			example.setUrl(interFace.getUrl());
			if(interfaceService.findByExample(example).size()>0){
				return new JsonResult(new BiyaoBizException("000004"));
			}
			interfaceService.save(interFace);
		}
		return new JsonResult(1, interFace);
	}

	@RequestMapping("/delete.do")
	@ResponseBody
	public JsonResult delete(@ModelAttribute Interface interFace) throws BiyaoBizException {
		interFace = interfaceService.get(interFace.getId());
		Tools.hasAuth(Const.AUTH_INTERFACE, request.getSession(), interFace.getModuleId());
		interfaceService.delete(interFace);
		return new JsonResult(1, null);
	}

}
package pe.joedayz.campus.controller;


import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Joiner;

import pe.joedayz.campus.dto.ModuleDto;
import pe.joedayz.campus.dto.UserDto;
import pe.joedayz.campus.rest.BackendRestInvoker;
import pe.joedayz.campus.service.intf.UserService;


@Controller
public class IndexController {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    @Value("${backend.server}")
    private String server;

    @Value("${backend.port}")
    private int port;

    @Value("${version}")
    private String version;

    @Value("${build}")
    private String build;

    @Value("${timestamp}")
    private String timestamp;

    @Value("${revision}")
    private String revision;

    @Value("${spring.sessionTimeOutMinutes}")
    private int sessionTimeOutMinutes;

    @Autowired
    UserService userService;

	@RequestMapping("/")
    ModelAndView index(){

        LOG.info("Is Remember me autenticated {} ", isRememberMeAuthenticated());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = ((UserDetails) auth.getPrincipal()).getUsername();

        BackendRestInvoker restInvoker = new BackendRestInvoker<List<UserDto>>(server,port);
        ResponseEntity<UserDto> responseEntity=
                restInvoker.sendGet("/user/findByName?username=" + ((UserDetails)auth.getPrincipal()).getUsername(), UserDto.class);
        UserDto userDto=responseEntity.getBody();

        BackendRestInvoker restEnumInvoker = new BackendRestInvoker<List<Map>>(server,port);
        ResponseEntity<Map> responseEnum=
                restEnumInvoker.sendGet("/generalTable/allEnums", Map.class);




        ModelAndView mav = new ModelAndView("index");
        mav.addObject("pages", buildPageInfo(username)); //build menu
        mav.addObject("allModules", buildAllPagesInfo(username)); //build navigation rules
        mav.addObject("username", username);
        mav.addObject("completeName", String.format("%s %s", userDto.getFirstName(), userDto.getLastName()));
        mav.addObject("initials", getInitials(userDto.getFirstName(), userDto.getLastName()));
        mav.addObject("version", version);
        mav.addObject("build", build);
        mav.addObject("timestamp", timestamp);
        mav.addObject("revision", revision);
        mav.addObject("sessionTimeOutMinutes", sessionTimeOutMinutes);
        mav.addObject("isRememberMeAuthenticated", isRememberMeAuthenticated());
        mav.addObject("hasRembermeCheck", userService.hasPersistentToken(username));

        mav.addObject("arrayEnums", responseEnum.getBody());


        return mav;
    }

    private boolean isRememberMeAuthenticated() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return RememberMeAuthenticationToken.class.isAssignableFrom(authentication.getClass());
    }

    private String getInitials(String firstName, String lastName) {
        if (StringUtils.isEmpty(firstName)) {
            return "CJ"; //Campus JoeDayz
        }
        String lastInitial = StringUtils.isEmpty(lastName) ? "" : lastName.substring(0, 1);
        return firstName.substring(0, 1) + lastInitial;
    }

	private List<ModuleDto> buildPageInfo(String username) {
        BackendRestInvoker restInvoker = new BackendRestInvoker<List<ModuleDto>>(server,port);

        ResponseEntity<ModuleDto[]> responseEntity2 =
                restInvoker.sendGet("/module/allowedModules?username=" + username, ModuleDto[].class);

        ModuleDto[] allowedVisbleModules = responseEntity2.getBody();

        LOG.info("Allowed modules: " + Joiner.on(",").join(allowedVisbleModules));

        return asList(allowedVisbleModules);
    }

    private List<ModuleDto> buildAllPagesInfo(String username) {
        BackendRestInvoker restInvoker = new BackendRestInvoker<List<ModuleDto>>(server,port);

        ResponseEntity<ModuleDto[]> responseEntity2 =
                restInvoker.sendGet("/module/allModulesWithPermission?username=" + username, ModuleDto[].class);

        ModuleDto[] allowedModules = responseEntity2.getBody();

        return asList(allowedModules);
    }


}

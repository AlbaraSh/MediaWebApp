package com.mediawebapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the Vite SPA for browser routes. API traffic stays on {@code /api/**}.
 */
@Controller
public class SpaController {

	@GetMapping({
			"/",
			"/login",
			"/register",
			"/discover",
			"/search",
			"/shelf",
			"/library",
			"/for-you",
			"/media/{id}"
	})
	public String spa() {
		return "forward:/index.html";
	}
}

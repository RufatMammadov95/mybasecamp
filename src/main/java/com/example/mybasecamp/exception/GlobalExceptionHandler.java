package com.example.mybasecamp.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxSizeException(MaxUploadSizeExceededException exc, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		redirectAttributes.addFlashAttribute("error", "The file size is too large! You can upload a maximum of 10MB.");
		redirectAttributes.addFlashAttribute("activePanel", "attachments");

		String referer = request.getHeader("Referer");
		return "redirect:" + (referer != null ? referer : "/projects");
	}

	@ExceptionHandler(RuntimeException.class)
	public String handleRuntimeException(RuntimeException exc, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		redirectAttributes.addFlashAttribute("error", "An error occurred.: " + exc.getMessage());
		String referer = request.getHeader("Referer");
		return "redirect:" + (referer != null ? referer : "/projects");
	}
}
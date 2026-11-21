package com.emu.codemodeval.web;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("sources", CodemodSource.values());
        model.addAttribute("ecosystems", Ecosystem.values());
        return "index";
    }
}

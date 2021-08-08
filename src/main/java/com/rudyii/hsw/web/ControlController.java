package com.rudyii.hsw.web;

import com.rudyii.hsw.objects.IamBack;
import com.rudyii.hsw.services.IamBackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/control")
@AllArgsConstructor
public class ControlController {
    private final IamBackService iamBackService;

    @PostMapping("/iam_back")
    public void iamBack(@RequestBody IamBack iamBack) {
        iamBackService.disarmMeBy(iamBack);
    }
}

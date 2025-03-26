package com.example.exambuddy.controller;

import com.example.exambuddy.model.RecordTopUser;
import com.example.exambuddy.service.LeaderBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
public class LeaderBoardController {
    @Autowired
    LeaderBoardService leaderBoardService;
    @GetMapping("/leaderboard")
    public String leaderBoard(Model model) {
        return "leaderboard";
    }
    @ResponseBody
    @GetMapping("/leaderboard-score")
    public CompletableFuture<List<RecordTopUser>> getUserScore() {
        return leaderBoardService.getUserScore();
    }
    @ResponseBody
    @GetMapping("/leaderboard-contribute")
    public CompletableFuture<List<RecordTopUser>> getUserContribute() {
        return leaderBoardService.getUserContribute();
    }
}

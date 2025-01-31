package com.dayquest.dayquestbackend.legals;


import com.dayquest.dayquestbackend.comment.CommentDTO;
import com.dayquest.dayquestbackend.quest.QuestDTO;
import com.dayquest.dayquestbackend.report.Report;
import com.dayquest.dayquestbackend.user.UserDTO;
import com.dayquest.dayquestbackend.video.VideoDTO;

import java.util.List;

public class DataDTO {
    private UserDTO user;
    private List<VideoDTO> uploadedVideos;
    private List<QuestDTO> postedQuests;
    private List<VideoDTO> viewedVideos;
    private List<QuestDTO> likedQuests;
    private List<QuestDTO> dislikedQuests;
    private List<VideoDTO> likedVideos;
    private List<VideoDTO> dislikedVideos;
    private List<CommentDTO> postedComments;
    private List<Report> reports;

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public List<VideoDTO> getUploadedVideos() {
        return uploadedVideos;
    }

    public void setUploadedVideos(List<VideoDTO> uploadedVideos) {
        this.uploadedVideos = uploadedVideos;
    }

    public List<QuestDTO> getPostedQuests() {
        return postedQuests;
    }

    public void setPostedQuests(List<QuestDTO> postedQuests) {
        this.postedQuests = postedQuests;
    }

    public List<VideoDTO> getViewedVideos() {
        return viewedVideos;
    }

    public void setViewedVideos(List<VideoDTO> viewedVideos) {
        this.viewedVideos = viewedVideos;
    }

    public List<QuestDTO> getLikedQuests() {
        return likedQuests;
    }

    public void setLikedQuests(List<QuestDTO> likedQuests) {
        this.likedQuests = likedQuests;
    }

    public List<QuestDTO> getDislikedQuests() {
        return dislikedQuests;
    }

    public void setDislikedQuests(List<QuestDTO> dislikedQuests) {
        this.dislikedQuests = dislikedQuests;
    }

    public List<VideoDTO> getLikedVideos() {
        return likedVideos;
    }

    public void setLikedVideos(List<VideoDTO> likedVideos) {
        this.likedVideos = likedVideos;
    }

    public List<VideoDTO> getDislikedVideos() {
        return dislikedVideos;
    }

    public void setDislikedVideos(List<VideoDTO> dislikedVideos) {
        this.dislikedVideos = dislikedVideos;
    }

    public List<CommentDTO> getPostedComments() {
        return postedComments;
    }

    public void setPostedComments(List<CommentDTO> postedComments) {
        this.postedComments = postedComments;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }
}

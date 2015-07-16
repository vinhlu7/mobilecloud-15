/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.*;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;

@Controller
public class AnEmptyController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it to
	 * something other than "AnEmptyController"
	 * 
	 * 
	 */
	@Autowired 
	private VideoFileManager videoDataRepository_;
	
	private static final AtomicLong currentId = new AtomicLong(0L);

	private Map<Long, Video> videos = new HashMap<Long, Video>();

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		checkAndSetId(v);
		videos.put(v.getId(), v);
		
		String target = getVideoUrl(v);
		v.setDataUrl(target);
		//v.setDataUrl(getDataUrl(v.getId()));
		return v;
	}

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideo(){
		return Lists.newArrayList(videos.values());
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody VideoStatus addVideoData(
			@PathVariable("id") long id,
			@RequestPart("data") MultipartFile videoData,
			HttpServletResponse mResponse) throws IOException{
		
		VideoStatus videoStatus = null;
		//InputStream in = videoData.getInputStream();
		Video video = videos.get(id);
		
		if(video != null){
			videoDataRepository_.saveVideoData(video, videoData.getInputStream());
			videoStatus = new VideoStatus(VideoState.READY);
			//mResponse.setContentType("text/html");
			//mResponse.setStatus(200);
			
		}else{
			//mResponse.setContentType("text/html");
			mResponse.sendError(404, "Video does not exist");
			
		}
		return videoStatus;
		
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public void downloadVideo(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, 
					HttpServletResponse mResponse) throws IOException{
		
		Video video = videos.get(id);
		if(video != null){
			//use the content type of the output stream to match
			//what was provided when vid was uploaded.
			mResponse.setContentType(video.getContentType());
			videoDataRepository_.copyVideoData(video, mResponse.getOutputStream());	
			
		}else{
			mResponse.sendError(404, "Video not found");
		}
	}
	
	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
	
	//replacing /{id}/ with the actual video's id
	private String getVideoUrl(Video v){
		String base = getUrlBaseForLocalServer();
		return base 
				+ VideoSvcApi.VIDEO_DATA_PATH.replace("{" + VideoSvcApi.ID_PARAMETER + "}", 
						"" + v.getId());
	}
}

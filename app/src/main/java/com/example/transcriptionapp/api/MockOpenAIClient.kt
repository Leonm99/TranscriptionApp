package com.example.transcriptionapp.com.example.transcriptionapp.api

import com.example.transcriptionapp.model.SettingsRepository
import java.io.File

class MockOpenAiHandler(private val settingsRepository: SettingsRepository) {


val text = "You're absolutely right, we're getting closer but still have a slight issue with the scroll boundaries. The problem now is that the onPreScroll is preventing the horizontal scroll when at the start or the end of the pages, but its doing it even when the pager has not yet fully settled on the target page.Let's refine the solution to address this:Problem Breakdown•Eager Boundary Check: The previous solution was a little too eager in checking for the start and end boundaries. It was blocking the horizontal swipe even during the transition animation when the pager was still moving towards the next page, or when it was close to the boundary.•Need for Tolerance: We need a little tolerance so that the pager can complete its transition animation before blocking the swipe.SolutionThe solution is to check if the offset of the page is close to 0 or to pageCount-1:Here's the updated code:"




    suspend fun whisper(file: File): String {

        return text }

    suspend fun summarize(userText: String): String {


        return text
    }

    suspend fun translate(userText: String): String {

      return text
    }

    suspend fun correctSpelling(userText: String): String {

        return text
    }




    suspend fun checkApiKey(apiKey: String): Boolean {

        return true
    }






}


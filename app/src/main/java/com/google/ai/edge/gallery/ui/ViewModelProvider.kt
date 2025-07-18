/*
 * Copyright 2025 Google LLC
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
 */

package com.google.ai.edge.gallery.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.ai.edge.gallery.GalleryApplication
import com.google.ai.edge.gallery.ui.imageclassification.ImageClassificationViewModel
import com.google.ai.edge.gallery.ui.imagegeneration.ImageGenerationViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmAskImageViewModel
import com.google.ai.edge.gallery.ui.llmsingleturn.LlmSingleTurnViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.textclassification.TextClassificationViewModel

object ViewModelProvider {
  val Factory = viewModelFactory {
    // Initializer for ModelManagerViewModel.
    initializer {
      val downloadRepository = galleryApplication().container.downloadRepository
      val dataStoreRepository = galleryApplication().container.dataStoreRepository
      ModelManagerViewModel(
        downloadRepository = downloadRepository,
        dataStoreRepository = dataStoreRepository,
        context = galleryApplication().container.context,
      )
    }

    // Initializer for TextClassificationViewModel
    initializer {
      TextClassificationViewModel()
    }

    // Initializer for ImageClassificationViewModel
    initializer {
      ImageClassificationViewModel()
    }

    // Initializer for LlmChatViewModel.
    initializer {
      // Assuming 'chatInteractionDao' is available in your application container
      val chatInteractionDao = galleryApplication().container.chatInteractionDao // MODIFY THIS LINE if the name is different
      LlmChatViewModel(chatInteractionDao = chatInteractionDao)
    }

    // Initializer for LlmSingleTurnViewModel..
    initializer {
      LlmSingleTurnViewModel()
    }

    // Initializer for LlmAskImageViewModel.
    initializer {
      // Assuming your DAO for LlmAskImageViewModel is named 'injectedDao'
      // and available in the container
      val anInjectedDao = galleryApplication().container.injectedDao // MODIFY THIS LINE if the name is different
      LlmAskImageViewModel(injectedDao = anInjectedDao)
    }

    // Initializer for ImageGenerationViewModel.
    initializer {
      ImageGenerationViewModel()
    }
  }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [GalleryApplication].
 */
fun CreationExtras.galleryApplication(): GalleryApplication =
  (this[AndroidViewModelFactory.APPLICATION_KEY] as GalleryApplication)

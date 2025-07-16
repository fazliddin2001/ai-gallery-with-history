package com.google.ai.edge.gallery.di // 1. Package

import android.content.Context // 2. Android Context
import com.google.ai.edge.gallery.db.AppDatabase // Your Room database class
import com.google.ai.edge.gallery.db.ChatInteractionDao // Your DAO interface
import dagger.Module // 3. Dagger/Hilt Annotation
import dagger.Provides // 4. Dagger/Hilt Annotation
import dagger.hilt.InstallIn // 5. Dagger/Hilt Annotation
import dagger.hilt.android.qualifiers.ApplicationContext // 6. Dagger/Hilt Annotation
import dagger.hilt.components.SingletonComponent // 7. Dagger/Hilt Component
import javax.inject.Singleton // 8. Javax Annotation (for scoping)

@Module // 9. This class is a Hilt Module
@InstallIn(SingletonComponent::class) // 10. Install this module's bindings in the SingletonComponent
object DatabaseModule { // 11. Kotlin object (singleton) for the module

    @Provides // 12. This function provides a dependency
    @Singleton // 13. Provide this dependency as a singleton (only one instance per component)
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase { // 14. Function signature
        // 15. How to create an AppDatabase:
        return AppDatabase.getDatabase(context)
    }

    @Provides // 16. This function also provides a dependency
    fun provideChatInteractionDao(appDatabase: AppDatabase): ChatInteractionDao { // 17. Function signature
        // 18. How to create a ChatInteractionDao:
        // Hilt will automatically see that this function needs an `AppDatabase`
        // and will use the `provideAppDatabase` function above to get it.
        return appDatabase.chatInteractionDao()
    }
}

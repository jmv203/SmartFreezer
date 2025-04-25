package com.example.smartfreezer.repository

import com.example.smartfreezer.RemoteDataSource
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class RecipeRepository @Inject constructor(
    remoteDataSource: RemoteDataSource,
){
    val remote = remoteDataSource
}
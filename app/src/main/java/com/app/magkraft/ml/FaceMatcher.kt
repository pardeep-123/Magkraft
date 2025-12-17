package com.app.magkraft.ml

import android.util.Log
import com.app.magkraft.data.local.db.UserEntity
import java.nio.ByteBuffer
import kotlin.math.sqrt

object FaceMatcher {

    // ✅ Correct threshold for normalized MobileFaceNet
    private const val THRESHOLD = 0.88f

    fun findBestMatch(
        currentEmbedding: FloatArray,
        users: List<UserEntity>
    ): UserEntity? {

        var bestScore = -1f
        var matchedUser: UserEntity? = null

        for (user in users) {

            val score = cosineSimilarity(
                currentEmbedding,
                user.embedding
            )

            if (score > bestScore) {
                bestScore = score
                matchedUser = user
            }
        }

        return if (bestScore >= THRESHOLD){
            Log.d( "findBestMatch: ", bestScore.toString())
            matchedUser

        } else{
            Log.d("findBestMatch: ", bestScore.toString())
            null
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot   // magnitudes = 1 because already normalized
    }
}


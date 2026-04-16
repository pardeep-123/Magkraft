package com.app.magkraft.ml

import android.util.Log
import com.app.magkraft.data.local.db.UserEntity
import java.nio.ByteBuffer
import kotlin.math.sqrt

object FaceMatcher {

    // ✅ Correct threshold for normalized MobileFaceNet
    private const val THRESHOLD = 0.75f

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
            Log.d("SCORES", "${user.name}: $score")  // 🔥 See ALL scores
            if (score > bestScore) {
                bestScore = score
                matchedUser = user
            }
            // 🔥 Optimization: If it's a near-perfect match, stop looking
            if (score > 0.95f) break
        }
        Log.d("BEST", "Best: $bestScore >= $THRESHOLD ? ${matchedUser?.name}")

        return if (bestScore >= THRESHOLD){
            Log.d( "findBestMatch: ", bestScore.toString())

            matchedUser

        } else{
            Log.d("findBestMatch: ", bestScore.toString())
            Log.d( "userSizeNumber: ", users.size.toString())

            null
        }
    }


    // In FaceMatcher, add normalization check
    private fun cosineSimilarity(a: FloatArray, b: FloatArray?): Float {

        if (b == null || a.size != b.size) return 0f // Safety check

        Log.d("VECTORS", "A: ${a[0]}, ${a[1]}, ${a[2]}")
        Log.d("VECTORS", "B: ${b[0]}, ${b[1]}, ${b[2]}")
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot
    }
}


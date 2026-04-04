package com.elites.fullcharge.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    companion object {
        private const val TAG = "AdManager"
        // 전면 광고 Ad Unit ID
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8531357339200043/5244697823"
        // 배너 광고 Ad Unit ID
        private const val BANNER_AD_UNIT_ID = "ca-app-pub-8531357339200043/4344018426"
        // 보상형 광고 Ad Unit ID (계급 복구용)
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-8531357339200043/1904919316"

        // 광고 활성화 여부 (false로 설정하면 광고 없이 바로 다음 화면으로)
        private const val ADS_ENABLED = true
    }

    /**
     * 전면 광고 미리 로드
     */
    fun loadInterstitialAd() {
        if (!ADS_ENABLED) return
        if (isLoading || interstitialAd != null) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "전면 광고 로드 완료")
                        interstitialAd = ad
                        isLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "전면 광고 로드 실패: ${error.message}")
                        interstitialAd = null
                        isLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "광고 로드 중 예외 발생: ${e.message}")
            isLoading = false
        }
    }

    /**
     * 전면 광고 표시 (퇴장 시 호출)
     * @param activity 현재 Activity
     * @param onAdDismissed 광고가 닫힌 후 실행할 콜백
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        // 광고 비활성화 시 바로 다음 화면으로
        if (!ADS_ENABLED) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd

        if (ad == null) {
            Log.d(TAG, "광고가 준비되지 않음, 바로 다음 화면으로")
            onAdDismissed()
            loadInterstitialAd() // 다음을 위해 로드
            return
        }

        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "광고 닫힘")
                    interstitialAd = null
                    loadInterstitialAd() // 다음 광고 미리 로드
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "광고 표시 실패: ${error.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "광고 표시됨")
                    interstitialAd = null // 표시된 광고는 재사용 불가
                }
            }

            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "광고 표시 중 예외 발생: ${e.message}")
            interstitialAd = null
            onAdDismissed()
        }
    }

    /**
     * 광고가 준비되었는지 확인
     */
    fun isAdReady(): Boolean = ADS_ENABLED && interstitialAd != null

    /**
     * 보상형 광고 미리 로드 (계급 복구용)
     */
    fun loadRewardedAd() {
        if (!ADS_ENABLED) return
        if (isRewardedLoading || rewardedAd != null) return

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()

        try {
            RewardedAd.load(
                context,
                REWARDED_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "보상형 광고 로드 완료")
                        rewardedAd = ad
                        isRewardedLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "보상형 광고 로드 실패: ${error.message}")
                        rewardedAd = null
                        isRewardedLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "보상형 광고 로드 중 예외 발생: ${e.message}")
            isRewardedLoading = false
        }
    }

    /**
     * 보상형 광고 표시 (계급 복구 시 호출)
     * @param activity 현재 Activity
     * @param onRewarded 광고 시청 완료 시 보상 콜백
     * @param onAdDismissed 광고가 닫힌 후 실행할 콜백
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onAdDismissed: () -> Unit) {
        if (!ADS_ENABLED) {
            onRewarded()
            onAdDismissed()
            return
        }

        val ad = rewardedAd

        if (ad == null) {
            Log.d(TAG, "보상형 광고가 준비되지 않음")
            onAdDismissed()
            loadRewardedAd()
            return
        }

        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "보상형 광고 닫힘")
                    rewardedAd = null
                    loadRewardedAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "보상형 광고 표시 실패: ${error.message}")
                    rewardedAd = null
                    loadRewardedAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "보상형 광고 표시됨")
                    rewardedAd = null
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "보상 획득: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "보상형 광고 표시 중 예외 발생: ${e.message}")
            rewardedAd = null
            onAdDismissed()
        }
    }

    /**
     * 보상형 광고가 준비되었는지 확인
     */
    fun isRewardedAdReady(): Boolean = ADS_ENABLED && rewardedAd != null

    /**
     * 배너 광고 Composable
     */
    @Composable
    fun BannerAd(modifier: Modifier = Modifier) {
        if (!ADS_ENABLED) return

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

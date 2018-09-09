package org.braincopy.esst;

import com.twitter.sdk.android.core.models.Tweet;

public interface MyAdapterListener {

    // リプライボタンが押されたことをActivityに知らせる
    public void onClickReplyButton(Tweet tweet);
}

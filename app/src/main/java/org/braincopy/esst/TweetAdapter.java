package org.braincopy.esst;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.twitter.sdk.android.core.models.Tweet;

import java.util.List;

public class TweetAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater layoutInflater = null;
    private List<Tweet> tweetList;
    // 追加
    private MyAdapterListener mListener;

    public TweetAdapter(Context context, List<Tweet> tweetList, MyAdapterListener mListener) {
        this.context = context;
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.tweetList = tweetList;
        // 追加
        this.mListener = mListener;
    }

    @Override
    public int getCount() {
        return tweetList.size();
    }

    @Override
    public Object getItem(int position) {
        return tweetList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return tweetList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.tweet_row, parent, false);

        final Tweet tweet = tweetList.get(position);

        TextView screenNameTextView = (TextView)convertView.findViewById(R.id.screen_name);
        TextView tweetTextTextView = (TextView)convertView.findViewById(R.id.tweet_text);
        TextView favoriteCountTextView = (TextView)convertView.findViewById(R.id.favorite_count);
    //    Button replyButton = (Button) convertView.findViewById(R.id.reply_button);

        screenNameTextView.setText(tweet.user.name);
        tweetTextTextView.setText(tweet.text);
        favoriteCountTextView.setText(String.valueOf(tweet.favoriteCount));

        // イベントを拾う
        /*
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ボタンが押されたら、独自リスナーのメソッドを呼ぶ。
                // 引数にはリプライ対象のtweetを渡す
                mListener.onClickReplyButton(tweet);
            }
        });
*/

        return convertView;
    }
}

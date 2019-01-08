package com.vvt.andon.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.vvt.andon.R;
import com.vvt.andon.activities.HomeActivity;
import com.vvt.andon.api_responses.allnotifications.NotificationList;
import com.vvt.andon.utils.APIServiceHandler;

import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyHolder> {

    private Context context;
    private List<NotificationList> mList;

    private int lastPosition = -1;

    NotificationInterface notificationInterface;

    public NotificationAdapter(Context context, List<NotificationList> mList) {
        this.context = context;
        this.mList = mList;
        notificationInterface= (NotificationInterface) context;

    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.new_notification_layout, parent, false);
        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position) {

        holder.mErrorID.setText(String.valueOf(mList.get(position).getNotificationId()));
        holder.mErrorMessage.setText(mList.get(position).getError());
        holder.mLine.setText( mList.get(position).getLineCode().toUpperCase());
        holder.mStation.setText(mList.get(position).getStation());
        holder.mTeam.setText(mList.get(position).getTeam());

        holder.mAcceptedby.setText(mList.get(position).getAcceptedBy());
        holder.mComplaintStatus.setText(mList.get(position).getNotificationStatus());
        holder.mDateTime.setText(mList.get(position).getCreatedDate());

        if (mList.get(position).getImageLink() != null) {
            if (mList.get(position).getImageLink().endsWith(".png")) {
                String imageUrl = "http://" + HomeActivity.ipAddress + ":8080/AndonWebservices/";
                Picasso.get()
                        .load(imageUrl + mList.get(position).getImageLink())
                        //.placeholder(R.drawable.background_drawable)
                        //.error(R.drawable.user)
                        //.resize(150,150)
                        .into(holder.mImage);
            }
        }

        // Here you apply the animation when the view is bound
        setAnimation(holder.itemView, position);

    }


    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate, int position) {

        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {

        return mList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }


    public class MyHolder extends RecyclerView.ViewHolder {

        public ImageView mImage;
        public TextView mErrorID, mErrorMessage, mLine,mStation, mTeam, mAcceptedby, mComplaintStatus, mDateTime;

        public MyHolder(View itemView) {
            super(itemView);

            mImage = itemView.findViewById(R.id.vI_working_image);

            mErrorID = itemView.findViewById(R.id.vT_error_id);
            mErrorMessage = itemView.findViewById(R.id.vT_error_message);
            mLine = itemView.findViewById(R.id.vT_line);
            mStation=itemView.findViewById(R.id.vT_station);
            mTeam = itemView.findViewById(R.id.vT_team);
            mAcceptedby = itemView.findViewById(R.id.vT_accepted_by);
            mComplaintStatus = itemView.findViewById(R.id.vT_complaint_status);
            mDateTime = itemView.findViewById(R.id.vT_date_and_time);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    int position = getAdapterPosition();
                    String notificationID = mList.get(position).getNotificationId().toString();
                    String notificationStatus = mList.get(position).getNotificationStatus().trim().replaceAll("^\"|\"$", "");
                    String team=mList.get(position).getTeam();

                    String department = HomeActivity.employeeDepartment;

                    if (notificationStatus.equalsIgnoreCase("Open")) {

                        //Interface method when Notification is in open state
                        notificationInterface.acceptError(notificationID,team);
                    }

                    if (notificationStatus.equalsIgnoreCase("CA Pending") && (department.contains("TEF"))) {

                        //Interface method when TEF team completes work and try to give to CA
                        notificationInterface.giveCA(notificationID,team);

                    }

                    if (notificationStatus.equalsIgnoreCase("CA Pending") && department.equals("LOM")) {

                        //Interface method when LOM team completes work and try to give to CA
                        notificationInterface.giveCA(notificationID,team);
                    }

                    if (notificationStatus.equalsIgnoreCase("CA Pending") && department.contains("FCM")) {

                        //Interface method when FCM team completes work and try to give to CA
                        notificationInterface.giveCA(notificationID,team);

                    } else if (notificationStatus.equalsIgnoreCase("CheckList Pending") && (department.contains("TEF"))) {

                        //Interface method when repair time takes morethan 30 mins
                        notificationInterface.checklist(notificationID);

                    } else if (notificationStatus.equalsIgnoreCase("MOE Comment Pending") && (department.contains("MOE"))) {

                        //Interface method when TEF team completes their work
                        notificationInterface.giveMOEComment(notificationID,team);

                    }

                }

            });

        }

    }

    public interface NotificationInterface
    {
        public void acceptError(String errorId,String team);
        public void giveCA(String errorId,String team);
        public void giveMOEComment(String errorId,String team);
        public void checklist(String errorId);

    }
}

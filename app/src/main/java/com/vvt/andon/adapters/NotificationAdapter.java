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
    public TextView mErrorId;

    private int lastPosition = -1;

    AlertDialog.Builder builder;
    AlertDialog alertDialog;
    String ACTION_URL ="";
    String CHECKLIST_URL="";
    String MOE_URL="";
    String MOE_URL1="";

    public NotificationAdapter(Context context, List<NotificationList> mList, TextView mErrorId) {
        this.context = context;
        this.mList = mList;
        this.mErrorId = mErrorId;

    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_single_row, parent, false);
        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position) {

        holder.mErrorID.setText(mList.get(position).getNotificationId() + "");
        holder.mErrorMessage.setText("Error  : " + mList.get(position).getError());
        holder.mPlaceOfErrorOccurrence.setText("LINE: " + mList.get(position).getLineCode() + "   &" + "  STATION: " + mList.get(position).getStation());
        holder.mTeam.setText("Team  : " + mList.get(position).getTeam());

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
        public TextView mErrorID, mErrorMessage, mPlaceOfErrorOccurrence, mTeam, mAcceptedby, mComplaintStatus, mDateTime;

        public MyHolder(View itemView) {
            super(itemView);

            mImage = itemView.findViewById(R.id.vI_working_image);

            mErrorID = itemView.findViewById(R.id.vT_error_id);
            mErrorMessage = itemView.findViewById(R.id.vT_error_message);
            mPlaceOfErrorOccurrence = itemView.findViewById(R.id.vT_place_of_error_occurence);
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
                    String accaptedBy=mList.get(position).getAcceptedBy();
                    String currentEmployee=HomeActivity.employeeName;
                    //String UserTeam=mList.get(position).getTeam();
                    String department = HomeActivity.employeeDepartment;
                    HomeActivity.employeeTeam = mList.get(position).getTeam();
                    //HomeActivity.employeeTeam=mList.get(position).getTeam();

                    if (notificationStatus.equalsIgnoreCase("Open")) {
                        mErrorId.setText(notificationID);
                    }

                    //mErrorId.setText(mList.get(position).getNotificationId().toString());

                    if (notificationStatus.equalsIgnoreCase("CA Pending") && (department.contains("TEF"))) {
                        showActionPopup(notificationID, HomeActivity.employeeID, HomeActivity.employeeTeam);

                    }

                    if (notificationStatus.equalsIgnoreCase("CA Pending") && department.equals("LOM")) {
                        showActionPopup(notificationID, HomeActivity.employeeID, HomeActivity.employeeTeam);

                    }

                    if (notificationStatus.equalsIgnoreCase("CA Pending") && department.contains("FCM")) {
                        showActionPopup(notificationID, HomeActivity.employeeID, HomeActivity.employeeTeam);

                    } else if (notificationStatus.equalsIgnoreCase("CheckList Pending") && (department.contains("TEF"))) {
                        showCheckListPopup(notificationID,HomeActivity.employeeID);

                    } else if (notificationStatus.equalsIgnoreCase("MOE Comment Pending") && (department.contains("MOE"))) {


                        //callMOEAPI

                        MOE_URL1 ="http://"+HomeActivity.ipAddress+":8080/AndonWebservices/rest/action/"+notificationID;
                        MOE_URL1 = MOE_URL1.replaceAll(" ", "%20");
                        MOE_URL1 = MOE_URL1.replaceAll(" ", "%20");

                        new MOEComment().execute();

                    }

                }

            });

        }
        class MOEComment extends AsyncTask<Void,Void,String>
        {
            ProgressDialog progressDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(MOE_URL1 ==null)
                {
                    Toast.makeText(context, "Empty url", Toast.LENGTH_SHORT).show();
                }
                else {
                    progressDialog=new ProgressDialog(context);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage("Updating...");
                    progressDialog.show();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {

                APIServiceHandler sh = new APIServiceHandler();

                // Making a request to url and getting response
                String jsonStr = sh.makeServiceCall(MOE_URL1, APIServiceHandler.GET);
                return jsonStr;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }
                if(s!=null)
                {
                    //Used to replace double quotes with white space character
                    String acpt=s.replaceAll("^\"|\"$", "");


                    if (acpt.equals("Server TimeOut")) {
                        Toast.makeText(context, acpt, Toast.LENGTH_LONG).show();
                    }
                    if (!acpt.equals("")) {
                        Toast.makeText(context, acpt, Toast.LENGTH_LONG).show();
                        String[] msg = acpt.split("/");
                        String resolvedMessage = "Resolver Error" + ": " + msg[2];
                        String msgID = msg[1];
                        String team = msg[3];
                        System.out.println("sdsd:" + team);
                        String tmm = team.replace("\"", "");
                        showMOEpoPup(resolvedMessage, msgID, tmm);
                        // recreate();

                    } else if (acpt.contains("false")) {
                        Toast.makeText(context, "server busy wait for a while", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, acpt, Toast.LENGTH_LONG).show();
                    }

                }

            }
        }

        private void showMOEpoPup(String resolvedMessage, final String notificationID,final String employeeTeam) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.moe_popup_layout, null);

            final EditText mComment = dialogView.findViewById(R.id.vE_mpl_entered_text);
            TextView mYes = dialogView.findViewById(R.id.vT_mpl_ok);
            TextView mNo = dialogView.findViewById(R.id.vT_mpl_cancel);
            TextView mMessage=dialogView.findViewById(R.id.vT_mpl_messagebody);

            mMessage.setText(resolvedMessage);

            builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            builder.setCancelable(false);

            alertDialog = builder.create();
            alertDialog.show();
            mYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //replaceAll(System.getProperty("line.separator"), "") is used to remove new line characters from entered text
                    String enteredText = mComment.getText().toString().trim().replaceAll(System.getProperty("line.separator"), "");

                    if(TextUtils.isEmpty(enteredText) || enteredText.length()<2)
                    {
                        Toast.makeText(context, "Enter Some closing comment", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        alertDialog.dismiss();
                        callMOEClosingAPI(notificationID,enteredText,HomeActivity.employeeID,employeeTeam);
                    }


                }
            });

            mNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });

        }

        private void callMOEClosingAPI(String notificationID, String enteredMessage, String employeeID, String employeeTeam) {

            MOE_URL ="http://"+HomeActivity.ipAddress+":8080/AndonWebservices/rest/action/closeIssue/"+notificationID+"/"+enteredMessage+"/"+employeeID+"/"+employeeTeam;
            MOE_URL = MOE_URL.replaceAll(" ", "%20");
            MOE_URL = MOE_URL.replaceAll(" ", "%20");

            new MOECloseComment().execute();

            //Toast.makeText(context, notificationID+" "+enteredMessage, Toast.LENGTH_SHORT).show();
        }

        class MOECloseComment extends AsyncTask<Void,Void,String>
        {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(MOE_URL ==null)
                {
                    Toast.makeText(context, "Empty url", Toast.LENGTH_SHORT).show();
                }
                else {
                    progressDialog=new ProgressDialog(context);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage("Updating...");
                    progressDialog.show();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {

                APIServiceHandler sh = new APIServiceHandler();

                // Making a request to url and getting response
                String jsonStr = sh.makeServiceCall(MOE_URL, APIServiceHandler.GET);
                return jsonStr;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }
                if(s!=null)
                {
                    String jsonStr=s.replaceAll("^\"|\"$", "");

                    if (jsonStr.equals("Server TimeOut")) {
                        Toast.makeText(context, jsonStr, Toast.LENGTH_LONG).show();
                    }
                    else if(jsonStr.contains("true"))
                    {
                        Toast.makeText(context, "Message saved", Toast.LENGTH_LONG).show();
                        //new HomeActivity().refreshPage();

                        context.startActivity(new Intent(context,HomeActivity.class));
                    }
                    else {
                        Toast.makeText(context, jsonStr.toString(), Toast.LENGTH_LONG).show();
                        //new HomeActivity().refreshPage();
                        context.startActivity(new Intent(context,HomeActivity.class));
                    }

                }
            }
        }

        private void showActionPopup(final String notificationID, final String employeeID, final String employeeTeam) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.containment_action_layout, null);

            final EditText mComment = dialogView.findViewById(R.id.vMLT_entered_text);
            TextView mYes = dialogView.findViewById(R.id.vT_cal_ok);
            TextView mNo = dialogView.findViewById(R.id.vT_cal_cancel);

            builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            builder.setCancelable(false);

            alertDialog = builder.create();
            alertDialog.show();

            mYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //replaceAll(System.getProperty("line.separator"), "") is used to remove new line characters from entered text
                    String enteredMessage = mComment.getText().toString().trim().replaceAll(System.getProperty("line.separator"), "");
                    if (TextUtils.isEmpty(enteredMessage) || enteredMessage.length() < 6) {
                        Toast.makeText(context, "What action have you taken to solve the issue ?", Toast.LENGTH_SHORT).show();
                    } else {
                        alertDialog.dismiss();
                        callContainmentActionAPI(notificationID,enteredMessage,employeeID,employeeTeam);

                    }

                }
            });

            mNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    alertDialog.dismiss();

                }
            });


        }


        private void callContainmentActionAPI(String notificationID, String enteredMessage, String employeeID, String employeeTeam) {

            ACTION_URL ="http://"+HomeActivity.ipAddress+":8080/AndonWebservices/rest/action/"+notificationID+"/"+enteredMessage+"/"+employeeID+"/"+employeeTeam;
            ACTION_URL = ACTION_URL.replaceAll(" ", "%20");
            ACTION_URL = ACTION_URL.replaceAll(" ", "%20");

            new SubmitContainmentAction().execute();

            //Toast.makeText(context, notificationID+" "+enteredMessage, Toast.LENGTH_SHORT).show();
        }

        class SubmitContainmentAction extends AsyncTask<Void,Void,String>
        {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(ACTION_URL ==null)
                {
                    Toast.makeText(context, "Empty url", Toast.LENGTH_SHORT).show();
                }
                else {
                     progressDialog=new ProgressDialog(context);
                     progressDialog.setCancelable(false);
                     progressDialog.setMessage("Updating...");
                     progressDialog.show();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {

                APIServiceHandler sh = new APIServiceHandler();

                // Making a request to url and getting response
                String jsonStr = sh.makeServiceCall(ACTION_URL, APIServiceHandler.GET);
                return jsonStr;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }
                if(s!=null)
                {
                    String jsonStr=s.replaceAll("^\"|\"$", "");

                    if (jsonStr.equals("Server TimeOut")) {
                        Toast.makeText(context, jsonStr, Toast.LENGTH_LONG).show();
                    }
                    else if(jsonStr.contains("true"))
                    {
                        Toast.makeText(context, "Message saved", Toast.LENGTH_LONG).show();
                        //new HomeActivity().refreshPage();
                        context.startActivity(new Intent(context,HomeActivity.class));
                        notifyDataSetChanged();
                    }
                    else {
                        Toast.makeText(context, jsonStr.toString(), Toast.LENGTH_LONG).show();
                    }

                }
            }
        }

        private void showCheckListPopup(final String notificationID, final String employeeID)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.check_list_layout, null);

            TextView mYes = dialogView.findViewById(R.id.vT_cll_yes);
            TextView mNo = dialogView.findViewById(R.id.vT_cll_no);

            builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            builder.setCancelable(false);

            alertDialog = builder.create();
            alertDialog.show();

            mYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                    callCheckListAPI(notificationID,"1",employeeID);

                }
            });

            mNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                    callCheckListAPI(notificationID,"0",employeeID);

                }
            });

        }
        private void callCheckListAPI(String notificationID, String response, String employeeID) {

            CHECKLIST_URL ="http://"+HomeActivity.ipAddress+":8080/AndonWebservices/rest/action/checklist/"+notificationID+"/"+response+"/"+employeeID;
            CHECKLIST_URL = CHECKLIST_URL.replaceAll(" ", "%20");
            CHECKLIST_URL = CHECKLIST_URL.replaceAll(" ", "%20");

            new CheckListAPI().execute();

            //Toast.makeText(context, notificationID+" "+enteredMessage, Toast.LENGTH_SHORT).show();
        }

        class CheckListAPI extends AsyncTask<Void,Void,String>
        {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(CHECKLIST_URL ==null)
                {
                    Toast.makeText(context, "Empty url", Toast.LENGTH_SHORT).show();
                }
                else {
                    progressDialog=new ProgressDialog(context);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage("Updating...");
                    progressDialog.show();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {

                APIServiceHandler sh = new APIServiceHandler();

                // Making a request to url and getting response
                String jsonStr = sh.makeServiceCall(CHECKLIST_URL, APIServiceHandler.GET);
                return jsonStr;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }
                if(s!=null)
                {
                    String jsonStr=s.replaceAll("^\"|\"$", "");

                    if (jsonStr.equals("Server TimeOut")) {
                        Toast.makeText(context, jsonStr, Toast.LENGTH_LONG).show();
                    }
                    else if(jsonStr.contains("true"))
                    {
                        context.startActivity(new Intent(context,HomeActivity.class));
                        Toast.makeText(context, "Need to Fill the checklist in Line", Toast.LENGTH_LONG).show();
                    }
                    else {
                        context.startActivity(new Intent(context,HomeActivity.class));
                        //Toast.makeText(context, "Need to Fill the checklist in Line", Toast.LENGTH_LONG).show();
                        //Toast.makeText(context, jsonStr.toString(), Toast.LENGTH_LONG).show();
                    }

                }
            }
        }

    }
}

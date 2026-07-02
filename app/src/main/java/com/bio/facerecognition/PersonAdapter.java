package com.bio.facerecognition;

import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

public class PersonAdapter extends ArrayAdapter<Person> {

    DBManager dbManager;
    public PersonAdapter(Context context, ArrayList<Person> personList) {
        super(context, 0, personList);

        dbManager = new DBManager(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Person person = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_person, parent, false);
        }

        TextView tvName = (TextView) convertView.findViewById(R.id.textName);
        ImageView faceView = (ImageView) convertView.findViewById(R.id.imageFace);
        EditText editText = (EditText) convertView.findViewById(R.id.editText);
        Context context = convertView.getContext();

        convertView.findViewById(R.id.buttonDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirm");
                builder.setMessage("Are you sure you want to remove <" + person.name + "> user?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbManager.deletePerson(DBManager.personList.get(position).name);
                        notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog if "No" is clicked
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        convertView.findViewById(R.id.buttonEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setVisibility(View.VISIBLE);
                tvName.setVisibility(View.GONE);
                editText.setText(tvName.getText());
                editText.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d("TAG", "onEditorAction: actionId=" + actionId + ", event=" + event);
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String newText = editText.getText().toString();
                    dbManager.updatePerson(person.name, newText);
                    editText.setVisibility(View.GONE);
                    tvName.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        tvName.setText(person.name);
        faceView.setImageBitmap(person.face);
        // Return the completed view to render on screen
        return convertView;
    }
}
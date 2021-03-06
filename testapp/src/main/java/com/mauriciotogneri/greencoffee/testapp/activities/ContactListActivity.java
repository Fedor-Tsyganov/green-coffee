package com.mauriciotogneri.greencoffee.testapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.mauriciotogneri.greencoffee.testapp.R;
import com.mauriciotogneri.greencoffee.testapp.adapters.ContactAdapter;
import com.mauriciotogneri.greencoffee.testapp.database.ContactDatabase;
import com.mauriciotogneri.greencoffee.testapp.model.Contact;

import java.util.List;

public class ContactListActivity extends AppCompatActivity
{
    private static final String PARAMETER_USERNAME = "parameter.username";

    public static Intent create(Context context, String username)
    {
        Intent intent = new Intent(context, ContactListActivity.class);
        intent.putExtra(PARAMETER_USERNAME, username);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        String username = getIntent().getStringExtra(PARAMETER_USERNAME);

        ContactDatabase contactDatabase = new ContactDatabase();
        List<Contact> contacts = contactDatabase.contacts(username);

        ListView listView = (ListView) findViewById(R.id.contacts_list);

        if (!contacts.isEmpty())
        {
            listView.setAdapter(new ContactAdapter(this, contacts));
            listView.setOnItemClickListener(new OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    Contact contact = (Contact) parent.getItemAtPosition(position);
                    onContactSelected(contact);
                }
            });
        }
        else
        {
            listView.setVisibility(View.GONE);

            TextView labelEmptyList = (TextView) findViewById(R.id.contacts_label_emptyList);
            labelEmptyList.setVisibility(View.VISIBLE);
        }
    }

    private void onContactSelected(Contact contact)
    {
        startActivity(DetailsActivity.create(this, contact.id()));
    }
}
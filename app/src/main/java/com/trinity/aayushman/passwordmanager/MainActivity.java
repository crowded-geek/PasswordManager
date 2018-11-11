package com.trinity.aayushman.passwordmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.ValueEventListener;

import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import se.simbio.encryption.Encryption;

public class MainActivity extends AppCompatActivity {
    private static final int AUISI = 456;
    public RecyclerView passView;
    public FloatingActionButton addPass;
    public RecyclerView.Adapter adapter;
    public List<PassEntry> list;
    public FirebaseDatabase database;
    public long uniquenn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().
                    createSignInIntentBuilder().
                    setIsSmartLockEnabled(true).
                    build(), AUISI);
        } else {
            Constants.UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            database = FirebaseDatabase.getInstance();
            ValueEventListener key = new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild("keyHash")){
                        Constants.keyHash = dataSnapshot.child("keyHash").getValue(String.class);
                        if(!Constants.gotTheKey) {
                            AlertDialog.Builder a = new AlertDialog.Builder(MainActivity.this);
                            a.setTitle("Enter Key");
                            View v = View.inflate(MainActivity.this, R.layout.enter_key, null);
                            a.setView(v);
                            final EditText e = v.findViewById(R.id.enter_key_et);
                            a.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (md5(e.getText().toString()).equals(Constants.keyHash)) {
                                        Constants.gotTheKey = true;
                                        Constants.key = e.getText().toString();
                                        passView = findViewById(R.id.passes);
                                        ValueEventListener vel = new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.hasChildren()) {
                                                    ArrayList<PassEntry> l = new ArrayList<>();
                                                    uniquenn = (long) dataSnapshot.child("uniquen").getValue();
                                                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                                                        if (i.hasChildren()) {
                                                            PassEntry e = new PassEntry(
                                                                    i.child("username").getValue(String.class),
                                                                    i.child("password").getValue(String.class),
                                                                    i.child("description").getValue(String.class),
                                                                    i.child("uniquen").getValue(Long.class)
                                                            );
                                                            l.add(e);
                                                        }
                                                    }
                                                    list = l;
                                                    adapter = new ListPassAdapter(MainActivity.this, list);
                                                    passView.setAdapter(adapter);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        };
                                        database.getReference().child(Constants.UID).child("passes").addValueEventListener(vel);
                                        addPass = findViewById(R.id.add_pass);
                                        addPass.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                                                View v = View.inflate(MainActivity.this, R.layout.edit_pass_entry, null);
                                                final EditText t = v.findViewById(R.id.username_et);
                                                final EditText p = v.findViewById(R.id.password_et);
                                                final EditText d = v.findViewById(R.id.description_et);
                                                b.setTitle("New Password");
                                                b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        addPassToDB(
                                                                t.getText().toString(),
                                                                p.getText().toString(),
                                                                d.getText().toString()
                                                        );
                                                    }
                                                });
                                                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        dialogInterface.dismiss();
                                                    }
                                                });
                                                b.setView(v);
                                                b.show();
                                            }
                                        });

                                        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                                        passView.setLayoutManager(linearLayoutManager);
                                        list = new ArrayList<>();
                                        adapter = new ListPassAdapter(MainActivity.this, list);
                                        passView.setAdapter(adapter);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Incorrect key, try again!", Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                }
                            });
                            a.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                            a.setCancelable(false);
                            a.show();
                        }
                    } else {
                            AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                            b.setTitle("Set-up the key");
                            View v = View.inflate(MainActivity.this, R.layout.add_key, null);
                            final EditText t = v.findViewById(R.id.key_et);
                            b.setView(v);
                            b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    database.getReference().child(Constants.UID).child("keyHash").
                                            setValue(md5(t.getText().toString()));
                                    Constants.keyHash = dataSnapshot.child("keyHash").getValue(String.class);
                                    Constants.key = t.getText().toString();
                                }
                            });
                            b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(MainActivity.this, "You can't use application without adding the key", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            });
                            b.show();
                            b.setCancelable(false);
                        }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            database.getReference().child(Constants.UID).addValueEventListener(key);
        }
    }

    public interface ClickListener{
        public void onClick(View view,int position);
        public void onLongClick(View view,int position);
    }

    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{

        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener){

            this.clicklistener=clicklistener;
            gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                    if(child!=null && clicklistener!=null){
                        clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child=rv.findChildViewUnder(e.getX(),e.getY());
            if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
                clicklistener.onClick(child,rv.getChildAdapterPosition(child));
            }

            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    private void deletePassToDB(String uid, String uniquen){
        database.getReference().child(uid).child("passes").child(uniquen).removeValue();
    }

    private void editPassToDB(String uid, String username, String password, String description, String uniquen){
        database.getReference().child(uid).child("passes").child(uniquen).child("username").setValue(username);
        database.getReference().child(uid).child("passes").child(uniquen).child("password").setValue(password);
        database.getReference().child(uid).child("passes").child(uniquen).child("description").setValue(description);
    }

    private void addPassToDB(final String username, final String password, final String description) {
                    String key = Constants.key;
                    long un;
                    if (uniquenn != 0) {
                        un = uniquenn + 1;
                        uniquenn = uniquenn + 1;
                    } else {
                        un = uniquenn + 2;
                        uniquenn = uniquenn + 2;
                    }
                    database.getReference().child(Constants.UID).child("passes").child("uniquen").setValue(un);
                    DatabaseReference ref = database.getReference().child(Constants.UID).child("passes")
                            .child(String.valueOf(un));
                    PassEntry e = new PassEntry();
                    try {
                        e = new PassEntry(
                                encryptData(username, key),
                                encryptData(password, key),
                                description, un
                        );
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                    ref.setValue(e);
          }

    private String decryptData(String data, String key){
        String salt = "namakShamakDaalDeteHain";
        byte[] iv = new byte[16];
        Encryption encryption = Encryption.getDefault(key, salt, iv);
        return encryption.decryptOrNull(data);
    }

    public String encryptData(String data, String key) throws Exception {
        String salt = "namakShamakDaalDeteHain";
        byte[] iv = new byte[16];
        Encryption encryption = Encryption.getDefault(key, salt, iv);
        return encryption.encryptOrNull(data);
    }

    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case AUISI:
                if (resultCode == RESULT_OK) {
                    Constants.UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    ValueEventListener key = new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild("keyHash")) {
                                Constants.keyHash = dataSnapshot.child("keyHash").getValue(String.class);
                                if(!Constants.gotTheKey){
                                    AlertDialog.Builder a = new AlertDialog.Builder(MainActivity.this);
                                    a.setTitle("Enter Key");
                                    View v = View.inflate(MainActivity.this, R.layout.enter_key, null);
                                    a.setView(v);
                                    final EditText e = v.findViewById(R.id.enter_key_et);
                                    a.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (md5(e.getText().toString()).equals(Constants.keyHash)) {
                                                Constants.gotTheKey = true;
                                                Constants.key = e.getText().toString();
                                                passView = findViewById(R.id.passes);
                                                ValueEventListener vel = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.hasChildren()) {
                                                            ArrayList<PassEntry> l = new ArrayList<>();
                                                            uniquenn = (long) dataSnapshot.child("uniquen").getValue();
                                                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                                                if (i.hasChildren()) {
                                                                    PassEntry e = new PassEntry(
                                                                            i.child("username").getValue(String.class),
                                                                            i.child("password").getValue(String.class),
                                                                            i.child("description").getValue(String.class),
                                                                            i.child("uniquen").getValue(Long.class)
                                                                    );
                                                                    l.add(e);
                                                                }
                                                            }
                                                            list = l;
                                                            adapter = new ListPassAdapter(MainActivity.this, list);
                                                            passView.setAdapter(adapter);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                };
                                                database.getReference().child(Constants.UID).child("passes").addValueEventListener(vel);
                                                addPass = findViewById(R.id.add_pass);
                                                addPass.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                                                        View v = View.inflate(MainActivity.this, R.layout.edit_pass_entry, null);
                                                        final EditText t = v.findViewById(R.id.username_et);
                                                        final EditText p = v.findViewById(R.id.password_et);
                                                        final EditText d = v.findViewById(R.id.description_et);
                                                        b.setTitle("New Password");
                                                        b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                addPassToDB(t.getText().toString(),
                                                                        p.getText().toString(),
                                                                        d.getText().toString()
                                                                );
                                                            }
                                                        });
                                                        b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                dialogInterface.dismiss();
                                                            }
                                                        });
                                                        b.show();
                                                    }
                                                });

                                                RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                                                passView.setLayoutManager(linearLayoutManager);
                                                list = new ArrayList<>();
                                                adapter = new ListPassAdapter(MainActivity.this, list);
                                                passView.setAdapter(adapter);

                                            } else {
                                                Toast.makeText(MainActivity.this, "Incorrect key, try again!", Toast.LENGTH_LONG).show();
                                                finish();
                                            }
                                        }
                                    });
                                    a.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    });
                                    a.setCancelable(false);
                                    a.show();
                                }
                            } else {
                                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                                b.setTitle("Set-up the key");
                                View v = View.inflate(MainActivity.this, R.layout.add_key, null);
                                final EditText t = v.findViewById(R.id.key_et);
                                b.setView(v);
                                b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        database.getReference().child(Constants.UID).child("keyHash").
                                                setValue(md5(t.getText().toString()));
                                        Constants.keyHash = dataSnapshot.child("keyHash").getValue(String.class);
                                        Constants.key = t.getText().toString();
                                    }
                                });
                                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Toast.makeText(MainActivity.this, "You can't use application without adding the key", Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                });
                                b.show();
                                b.setCancelable(false);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    database = FirebaseDatabase.getInstance();
                    database.getReference().child(Constants.UID).addValueEventListener(key);
                    database.getReference().child(Constants.UID).child("passes").child("uniquen").setValue((long) 0);
                }
                break;
        }
    }

    class ListPassAdapter extends RecyclerView.Adapter<ListPassAdapter.PassEntryViewHolder> {

        public  Context context;
        public List<PassEntry> list;

        public ListPassAdapter(Context context, List<PassEntry> list){
            this.context = context;
            this.list = list;
        }

        @Override
        public ListPassAdapter.PassEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pass_entry, parent, false);
            return new PassEntryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PassEntryViewHolder holder, final int position) {
            final String username = decryptData(list.get(position).username, Constants.key);
            final String password = decryptData(list.get(position).password, Constants.key);
            final String description = list.get(position).description;

            holder.username.setText(username);
            holder.password.setText(password);
            holder.description.setText(description);
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    b.setTitle("Delete");
                    b.setMessage("Are you sure want to delete this password?");
                    b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deletePassToDB(Constants.UID, String.valueOf(list.get(position).uniquen));
                        }
                    });
                    b.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    b.show();
                }
            });

            holder.edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String key = Constants.key;
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    View v = View.inflate(context, R.layout.edit_pass_entry, null);
                    final EditText username = v.findViewById(R.id.username_et);
                    final EditText password = v.findViewById(R.id.password_et);
                    final EditText description = v.findViewById(R.id.description_et);
                    username.setText(decryptData(list.get(position).username, key));
                    password.setText(decryptData(list.get(position).password, key));
                    description.setText(list.get(position).description);
                    b.setView(v);
                    b.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                editPassToDB(Constants.UID,
                                        encryptData(username.getText().toString(), key),
                                        encryptData(password.getText().toString(), key),
                                        description.getText().toString(),
                                        String.valueOf(list.get(position).uniquen)
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    b.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class PassEntryViewHolder extends RecyclerView.ViewHolder {
            public TextView username, password, description;
            public ImageView delete, edit;
            public PassEntryViewHolder(View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username_tv);
                password = itemView.findViewById(R.id.password_tv);
                description = itemView.findViewById(R.id.description_tv);
                delete = itemView.findViewById(R.id.delete_iv);
                edit = itemView.findViewById(R.id.edit_iv);
            }
        }
    }
}
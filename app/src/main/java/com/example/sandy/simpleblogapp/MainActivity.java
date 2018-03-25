package com.example.sandy.simpleblogapp;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private RecyclerView blogList;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseUsers;

    private DatabaseReference mDatabaseLike;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private boolean mProgressLike=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth=FirebaseAuth.getInstance();
        mAuthListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if(firebaseAuth.getCurrentUser()==null){

                    Intent loginIntent=new Intent(MainActivity.this,LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                }

            }
        };


        mDatabase= FirebaseDatabase.getInstance().getReference().child("Blog");
        mDatabaseUsers=FirebaseDatabase.getInstance().getReference().child("Users");
        mDatabaseLike=FirebaseDatabase.getInstance().getReference().child("Likes");

        mDatabase.keepSynced(true);
        mDatabaseUsers.keepSynced(true);
        mDatabaseLike.keepSynced(true);

        blogList=(RecyclerView)findViewById(R.id.blog_list);
        blogList.setHasFixedSize(true);
        blogList.setLayoutManager(new LinearLayoutManager(this));

        checkUserExist();

    }

    @Override
    protected void onStart() {
        super.onStart();

        checkUserExist();

        mAuth.addAuthStateListener(mAuthListener);

        FirebaseRecyclerAdapter<Blog,BlogViewHolder> firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(

                Blog.class,
                R.layout.blog_row,
                BlogViewHolder.class,
                mDatabase
        ) {
            @Override
            protected void populateViewHolder(BlogViewHolder viewHolder, Blog model, int position) {

                final String post_key=getRef(position).getKey();

                viewHolder.setTitle(model.getTitle());
                viewHolder.setDesc(model.getDesc());
                viewHolder.setImage(getApplicationContext(),model.getImage());
                viewHolder.setUsername(model.getUsername());

                viewHolder.setLikeBtn(post_key);

                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Toast.makeText(MainActivity.this,post_key, Toast.LENGTH_SHORT).show();
                    }
                });

                viewHolder.mLikeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mProgressLike=true;

                        mDatabaseLike.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if (mProgressLike){

                                    if(dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())){

                                        mDatabaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();

                                        mProgressLike=false;
                                    }else {

                                        mDatabaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).setValue("RandeomValue");

                                        mProgressLike=false;
                                    }


                                }


                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }
                });


            }
        };

        blogList.setAdapter(firebaseRecyclerAdapter);
    }
    private void checkUserExist() {

        FirebaseUser user=mAuth.getCurrentUser();
        if (user==null){

            Intent userlogin=new Intent(MainActivity.this,LoginActivity.class);
            startActivity(userlogin);

        }else {

            final String user_id = mAuth.getCurrentUser().getUid();
            mDatabaseUsers.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (!dataSnapshot.hasChild(user_id)) {

                        Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);


                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    public static class BlogViewHolder extends RecyclerView.ViewHolder{

        View mView;

        private ImageButton mLikeBtn;

        DatabaseReference mDatabaseLike;
        FirebaseAuth mAuth;

        public BlogViewHolder(View itemView) {
            super(itemView);

            mView=itemView;

            mLikeBtn=(ImageButton)mView.findViewById(R.id.post_like);

            mDatabaseLike=FirebaseDatabase.getInstance().getReference().child("Likes");
            mAuth=FirebaseAuth.getInstance();

            mDatabaseLike.keepSynced(true);
        }

        public void setLikeBtn(final String post_key){

            mDatabaseLike.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if(dataSnapshot.child(post_key).hasChild(mAuth.getUid())){

                        mLikeBtn.setImageResource(R.drawable.likeheart);

                    }else {


                        mLikeBtn.setImageResource(R.drawable.defaultheart);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        public void setTitle(String title){

            TextView post_title=(TextView)mView.findViewById(R.id.post_title);
            post_title.setText(title);
        }

        public void setDesc(String desc){

            TextView post_desc=(TextView)mView.findViewById(R.id.post_desc);
            post_desc.setText(desc);
        }

        public void setUsername(String username){

            TextView post_username=(TextView)mView.findViewById(R.id.post_username);
            post_username.setText(username);

        }

        public void setImage(Context ctx,String image){

            ImageView post_image=(ImageView)mView.findViewById(R.id.post_image);
            Picasso.with(ctx).load(image).into(post_image);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId()==R.id.action_add){

            startActivity(new Intent(MainActivity.this,PostActivity.class));

        }
        if(item.getItemId()==R.id.action_logout) {

            logout();

        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {

        mAuth.signOut();
    }
}

package com.ifs21010.glostandfound.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ifs21010.glostandfound.MyAdapter
import com.ifs21010.glostandfound.R
import com.ifs21010.glostandfound.RetrofitObject
import com.ifs21010.glostandfound.data.Api
import com.ifs21010.glostandfound.data.LostfoundViewModel
import com.ifs21010.glostandfound.models.GetAllLostAndFoundsResponse
import com.ifs21010.glostandfound.models.GetCurrentUserResponse
import com.ifs21010.glostandfound.models.LostFound
import com.ifs21010.glostandfound.models.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var retrofit: Retrofit
    private lateinit var apiService: Api
    private lateinit var authToken: String
    private lateinit var currentUser: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        retrofit = RetrofitObject().getRetrofit()
        apiService = retrofit.create(Api::class.java)
        recyclerView = view.findViewById(R.id.lostfound_cycle)

        recyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)

        val sharedPref = context?.getSharedPreferences(
            "my_prefs_file",
            Context.MODE_PRIVATE
        )

        val myValue = sharedPref?.getString("auth_token", "")

        authToken = "Bearer $myValue"

        getCurrentUser()
        loadData()
    }

    fun loadData() {
        view?.findViewById<LinearProgressIndicator>(R.id.progress_horizontal)?.visibility =
            View.VISIBLE
        var isCompleted = arguments?.getInt("isCompleted")
        var isMe = arguments?.getInt("isMe")
        val status = arguments?.getString("status")

        if (isCompleted == -1) {
            isCompleted = null;
        }

        if (isMe == -1) {
            isMe = null;
        }

        Log.d("gabriel_log", "$isCompleted, $isMe, $status")
        val call = apiService.getLostFounds(authToken, isCompleted, isMe, status)

        val marked = arguments?.getBoolean("marked")

        call.enqueue(object : Callback<GetAllLostAndFoundsResponse> {
            @SuppressLint("FragmentLiveDataObserve")
            override fun onResponse(
                p0: Call<GetAllLostAndFoundsResponse>,
                p1: Response<GetAllLostAndFoundsResponse>
            ) {
                Log.d("gabriel_tag", p1.body()?.data!!.lostFounds.size.toString())
                try {
                    val arrayTemp = p1.body()?.data!!.lostFounds
                    val listSaved = ViewModelProvider(this@HomeFragment)[LostfoundViewModel::class.java]

                    if (marked!!) {
                        listSaved.allLostFoundId.observe(this@HomeFragment) { item ->
                            val iterator = arrayTemp.iterator()
                            while (iterator.hasNext()) {
                                val e = iterator.next()
                                if (!item.contains(e.id)) {
                                    iterator.remove()
                                }
                            }
                        }
                    }

                    arrayTemp.reverse()

                    view!!.findViewById<TextInputLayout>(R.id.until).hint = "Until (Max ${arrayTemp.size})"
                    val start = view!!.findViewById<TextInputEditText>(R.id.input_from).text.toString().toInt()
                    val end = view!!.findViewById<TextInputEditText>(R.id.input_until).text.toString().toInt()

                    val firstTenElements : List<LostFound> = try {
                        arrayTemp.subList(start - 1, minOf(arrayTemp.size, end))
                    } catch (e : IllegalArgumentException) {
                        arrayTemp.subList(0, minOf(arrayTemp.size, 5))
                    }

                    val customAdapter = MyAdapter(
                        firstTenElements,
                        context,
                        apiService,
                        authToken,
                        currentUser.name,
                        this@HomeFragment,
                        listSaved
                    )

                    recyclerView.adapter = customAdapter

                view?.findViewById<LinearProgressIndicator>(R.id.progress_horizontal)?.hide()
                } catch (e: UninitializedPropertyAccessException) {
                    Toast.makeText(context, "Reload page", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onFailure(p0: Call<GetAllLostAndFoundsResponse>, p1: Throwable) {
                Toast.makeText(context, "Gagal memuat data. Refresh!", Toast.LENGTH_LONG).show()
                Log.e("my_error", p1.toString())
            }
        })
    }

    private fun getCurrentUser() {
        val call = apiService.getCurrentUser(authToken)

        call.enqueue(object : Callback<GetCurrentUserResponse> {
            override fun onResponse(
                p0: Call<GetCurrentUserResponse>,
                p1: Response<GetCurrentUserResponse>
            ) {
                currentUser = p1.body()?.data!!.user
            }

            override fun onFailure(p0: Call<GetCurrentUserResponse>, p1: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }
}
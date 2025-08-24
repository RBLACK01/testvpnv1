package com.raindata.rainvpn

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var api: Api
    private var token: String? = null
    private var servers: List<Server> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val ok = OkHttpClient.Builder().addInterceptor(log).build()
        api = Retrofit.Builder()
            .baseUrl("http://vpn.raindatasolutions.com:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(ok)
            .build().create(Api::class.java)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnFetch = findViewById<Button>(R.id.btnFetchServers)
        val spinner = findViewById<Spinner>(R.id.spinnerServers)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val status = findViewById<TextView>(R.id.status)
        val cfg = findViewById<TextView>(R.id.configPreview)

        btnLogin.setOnClickListener {
            api.login(LoginReq(email.text.toString(), password.text.toString()))
                .enqueue(object : Callback<LoginResp> {
                    override fun onResponse(call: Call<LoginResp>, resp: Response<LoginResp>) {
                        if (resp.isSuccessful && resp.body()?.token != null) {
                            token = resp.body()!!.token
                            status.text = "Login OK"
                        } else status.text = "Login failed"
                    }
                    override fun onFailure(call: Call<LoginResp>, t: Throwable) {
                        status.text = "Error: ${t.message}"
                    }
                })
        }

        btnFetch.setOnClickListener {
            api.servers().enqueue(object : Callback<ServersResp> {
                override fun onResponse(call: Call<ServersResp>, resp: Response<ServersResp>) {
                    if (resp.isSuccessful) {
                        servers = resp.body()?.servers ?: emptyList()
                        val labels = servers.map { "${it.name} – ${it.city}" }
                        spinner.adapter = ArrayAdapter(this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item, labels)
                        status.text = "Servers loaded (${servers.size})"
                    } else status.text = "Servers failed"
                }
                override fun onFailure(call: Call<ServersResp>, t: Throwable) {
                    status.text = "Error: ${t.message}"
                }
            })
        }

        btnConnect.setOnClickListener {
            val tk = token ?: run { status.text = "Login first"; return@setOnClickListener }
            if (servers.isEmpty()) { status.text = "Fetch servers first"; return@setOnClickListener }

            // Keypair (MVP: sahte/base64 32 byte) — gerçek WireGuard anahtarı app’e eklerken değiştiririz
            val priv = Base64.getEncoder().encodeToString(Random.Default.nextBytes(32))
            val pub  = Base64.getEncoder().encodeToString(Random.Default.nextBytes(32))

            val choice = servers[spinner.selectedItemPosition]
            status.text = "Registering on ${choice.name}…"

            api.register("Bearer $tk", DeviceReq(name = "android-" + Random.nextInt(1000), pubkey = pub))
                .enqueue(object : Callback<DeviceResp> {
                    override fun onResponse(call: Call<DeviceResp>, resp: Response<DeviceResp>) {
                        if (resp.isSuccessful) {
                            val body = resp.body()!!
                            val filled = body.wg_conf_template.replace("<CLIENT_PRIVATE_KEY>", priv)
                            cfg.text = filled
                            status.text = "Config ready for ${choice.name}"
                        } else status.text = "Register failed"
                    }
                    override fun onFailure(call: Call<DeviceResp>, t: Throwable) {
                        status.text = "Error: ${t.message}"
                    }
                })
        }
    }
}

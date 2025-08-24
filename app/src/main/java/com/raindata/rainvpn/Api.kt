package com.raindata.rainvpn

import retrofit2.Call
import retrofit2.http.*

data class LoginReq(val email:String, val password:String)
data class LoginResp(val ok:Boolean, val token:String?)
data class Server(val id:String, val name:String, val city:String, val country_code:String,
                  val hostname:String, val port:Int, val protocol:String, val probe_url:String)
data class ServersResp(val ok:Boolean, val servers:List<Server>)
data class DeviceReq(val name:String, val pubkey:String)
data class DeviceResp(val ok:Boolean, val device_id:Int, val assigned_ip:String,
                      val server_pubkey:String, val wg_conf_template:String)

interface Api {
    @POST("v1/login")
    fun login(@Body body: LoginReq): Call<LoginResp>

    @GET("v1/servers")
    fun servers(): Call<ServersResp>

    @POST("v1/devices")
    fun register(@Header("Authorization") bearer:String, @Body body: DeviceReq): Call<DeviceResp>
}

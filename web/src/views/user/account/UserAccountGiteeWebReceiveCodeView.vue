<template>
    <div></div>
</template>

<script>
import router from '@/router/index'
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import $ from 'jquery'
import { serverIp } from '../../../config';

export default {
    setup() {
        const myRoute = useRoute();
        const store = useStore();

        $.ajax({
          url: "http://" + serverIp + ":3000/api/user/account/gitee/web/receive_code/",
          type: "GET",
          data: {
            code: myRoute.query.code,
            state: myRoute.query.state,
          },
          success: resp => {
            console.log("AJAX success callback triggered");
            console.log("Response received:", resp);

            if (resp.result === "success") {
              console.log("Third-party login successful");
              localStorage.setItem("jwt_token", resp.jwt_token);
              store.commit("updateToken", resp.jwt_token);
              router.push({ name: "home" });
              store.commit("updatePullingInfo", false);
            } else {
              console.log("Third-party login failed");
              router.push({ name: "user_account_login" });
            }
          },
          error: function(jqXHR, textStatus, errorThrown) {
            console.log("AJAX error callback triggered");
            console.log('Error:', textStatus, errorThrown);
            console.log('jqXHR:', jqXHR);
            console.log('errorThrown:', errorThrown);
            router.push({ name: "home" });
          },
          complete: function() {
            console.log("AJAX request completed");
          }
       });

    }
}
</script>

<style scoped>

</style>

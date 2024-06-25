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
            type: "post",
            data: {
                code: myRoute.query.code,
            },
            success: resp => {
                router.push({ name: "home" });
                console.log("hhhhhhh")
                if (resp.result === "success") {
                    console.log("第三方登录成功了")
                    localStorage.setItem("jwt_token", resp.jwt_token);
                    store.commit("updateToken", resp.jwt_token);
                    router.push({ name: "home" });
                    store.commit("updatePullingInfo", false);
                } else {
                    console.log("第三方登录失败了")
                    router.push({ name: "user_account_login" });
                }
            },
            error: () => {
                router.push({ name: "home" });
                console.log("shibai")
            },
            complete: () => {
                console.log("wanchengle")
            }
        })
    }
}
</script>

<style scoped>

</style>

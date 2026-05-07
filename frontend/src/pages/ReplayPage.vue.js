import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getReplay } from "../api/game";
const route = useRoute();
const router = useRouter();
const text = ref("");
const chapters = ref([]);
const loading = ref(false);
const error = ref("");
const sessionId = String(route.params.sessionId || "");
async function loadReplay() {
    loading.value = true;
    error.value = "";
    try {
        text.value = await getReplay(sessionId);
        chapters.value = text.value
            .split(/\n\s*\n/g)
            .map((chunk) => chunk.trim())
            .filter(Boolean);
    }
    catch (e) {
        error.value = e instanceof Error ? e.message : "回放获取失败";
    }
    finally {
        loading.value = false;
    }
}
onMounted(loadReplay);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['chapter']} */ ;
/** @type {__VLS_StyleScopedClasses['replay-head']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-wrap replay-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "panel replay-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
(__VLS_ctx.sessionId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.router.push(`/game/${__VLS_ctx.sessionId}`);
        } },
    ...{ class: "btn" },
});
if (!__VLS_ctx.loading && !__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
        ...{ class: "panel replay-body" },
    });
    if (__VLS_ctx.chapters.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "novel" },
        });
        for (const [chapter, idx] of __VLS_getVForSourceType((__VLS_ctx.chapters))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
                ...{ class: "chapter" },
                key: (`${idx}-${chapter.slice(0, 16)}`),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
            (chapter);
        }
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "empty" },
        });
    }
}
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "panel replay-body" },
    });
}
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "panel replay-body error" },
    });
    (__VLS_ctx.error);
}
/** @type {__VLS_StyleScopedClasses['page-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['replay-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['replay-head']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['replay-body']} */ ;
/** @type {__VLS_StyleScopedClasses['novel']} */ ;
/** @type {__VLS_StyleScopedClasses['chapter']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['replay-body']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['replay-body']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            router: router,
            chapters: chapters,
            loading: loading,
            error: error,
            sessionId: sessionId,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=ReplayPage.vue.js.map
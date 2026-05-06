import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { getDefaultWorld, getWorldFactoryJob, initializeWorld } from "../api/world";
const router = useRouter();
const loading = ref(false);
const polling = ref(false);
const error = ref("");
const notice = ref("");
const defaultWorldVersion = ref("world_v1");
const lastJob = ref(null);
const form = reactive({
    worldTheme: "废土科技衰败都市",
    eraStyle: "近未来崩坏",
    survivalTone: "高压求生",
    keyFaction: "拾荒者联盟",
    forbiddenRule: "禁止直接设定超自然无代价复活",
});
let pollTimer = null;
const worldVersionCandidate = computed(() => {
    if (lastJob.value?.worldVersion && lastJob.value.worldVersion.length > 0) {
        return lastJob.value.worldVersion;
    }
    return defaultWorldVersion.value;
});
function clearPollTimer() {
    if (pollTimer !== null) {
        window.clearInterval(pollTimer);
        pollTimer = null;
    }
}
function savePreferredWorldVersion(worldVersion) {
    localStorage.setItem("doomsday:preferredWorldVersion", worldVersion);
}
async function loadDefault() {
    try {
        const data = await getDefaultWorld();
        defaultWorldVersion.value = data.worldVersion;
    }
    catch (e) {
        defaultWorldVersion.value = "world_v1";
    }
}
async function pollJob(jobId) {
    try {
        const data = await getWorldFactoryJob(jobId);
        lastJob.value = data;
        if (data.status === "DONE") {
            savePreferredWorldVersion(data.worldVersion);
            notice.value = `世界生成完成，已切换为 ${data.worldVersion}`;
            polling.value = false;
            clearPollTimer();
        }
        if (data.status === "FAILED") {
            polling.value = false;
            clearPollTimer();
            error.value = data.errorMessage || "世界工厂任务失败";
        }
    }
    catch (e) {
        polling.value = false;
        clearPollTimer();
        error.value = e instanceof Error ? e.message : "查询任务状态失败";
    }
}
async function startInitialize() {
    if (loading.value || polling.value) {
        return;
    }
    loading.value = true;
    error.value = "";
    notice.value = "";
    clearPollTimer();
    try {
        const data = await initializeWorld(form);
        notice.value = `任务已提交：${data.jobId}`;
        polling.value = true;
        await pollJob(data.jobId);
        if (polling.value) {
            pollTimer = window.setInterval(() => {
                pollJob(data.jobId).catch(() => { });
            }, 2500);
        }
    }
    catch (e) {
        error.value = e instanceof Error ? e.message : "提交世界初始化任务失败";
    }
    finally {
        loading.value = false;
    }
}
function applyDefaultAndBack() {
    savePreferredWorldVersion(defaultWorldVersion.value);
    router.push("/");
}
function useCurrentAndBack() {
    savePreferredWorldVersion(worldVersionCandidate.value);
    router.push("/");
}
onMounted(() => {
    loadDefault().catch(() => { });
});
onBeforeUnmount(() => {
    clearPollTimer();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['kv']} */ ;
/** @type {__VLS_StyleScopedClasses['kv']} */ ;
/** @type {__VLS_StyleScopedClasses['status-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['layout']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-wrap world-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "panel topbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.router.push('/');
        } },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.useCurrentAndBack) },
    ...{ class: "btn btn--accent" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel block form-block" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "64",
});
(__VLS_ctx.form.worldTheme);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "64",
});
(__VLS_ctx.form.eraStyle);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "64",
});
(__VLS_ctx.form.survivalTone);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "64",
});
(__VLS_ctx.form.keyFaction);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "120",
});
(__VLS_ctx.form.forbiddenRule);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "submit-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.startInitialize) },
    ...{ class: "btn btn--accent" },
    disabled: (__VLS_ctx.loading || __VLS_ctx.polling),
});
(__VLS_ctx.loading ? "提交中..." : __VLS_ctx.polling ? "生成中..." : "提交初始化任务");
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel block status-block" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kv" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.defaultWorldVersion);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "kv" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.worldVersionCandidate);
if (__VLS_ctx.lastJob) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kv" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.lastJob.jobId);
}
if (__VLS_ctx.lastJob) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "kv" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.lastJob.status);
    (__VLS_ctx.lastJob.progress);
}
if (__VLS_ctx.lastJob?.stage) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "hint" },
    });
    (__VLS_ctx.lastJob.stage);
}
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
    });
    (__VLS_ctx.notice);
}
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "error" },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "status-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.applyDefaultAndBack) },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.useCurrentAndBack) },
    ...{ class: "btn btn--accent" },
});
/** @type {__VLS_StyleScopedClasses['page-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['world-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--accent']} */ ;
/** @type {__VLS_StyleScopedClasses['layout']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-block']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['submit-row']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--accent']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['block']} */ ;
/** @type {__VLS_StyleScopedClasses['status-block']} */ ;
/** @type {__VLS_StyleScopedClasses['kv']} */ ;
/** @type {__VLS_StyleScopedClasses['kv']} */ ;
/** @type {__VLS_StyleScopedClasses['kv']} */ ;
/** @type {__VLS_StyleScopedClasses['kv']} */ ;
/** @type {__VLS_StyleScopedClasses['hint']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['status-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--accent']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            router: router,
            loading: loading,
            polling: polling,
            error: error,
            notice: notice,
            defaultWorldVersion: defaultWorldVersion,
            lastJob: lastJob,
            form: form,
            worldVersionCandidate: worldVersionCandidate,
            startInitialize: startInitialize,
            applyDefaultAndBack: applyDefaultAndBack,
            useCurrentAndBack: useCurrentAndBack,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=WorldFactoryPage.vue.js.map
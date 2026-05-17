import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { createSession, getSessionState } from "../api/game";
import { getDefaultWorld } from "../api/world";
const router = useRouter();
const loading = ref(false);
const error = ref("");
const resumeSessionId = ref("");
const form = reactive({
    playerId: "u_1001",
    difficulty: "SURVIVOR",
    worldVersion: "world_v1",
    styleProfile: "grim_realism",
});
const difficultyOptions = [
    { value: "SEEKER", title: "求生", desc: "压力可控，恢复窗口更友好。" },
    { value: "SURVIVOR", title: "幸存", desc: "标准废土体验，风险与收益均衡。" },
    { value: "HELL", title: "地狱", desc: "资源稀缺且高压频发，容错极低。" },
];
async function loadWorldVersion() {
    const preferred = localStorage.getItem("doomsday:preferredWorldVersion");
    if (preferred && preferred.trim().length > 0) {
        form.worldVersion = preferred.trim();
        return;
    }
    try {
        const data = await getDefaultWorld();
        form.worldVersion = data.worldVersion;
    }
    catch {
        form.worldVersion = "world_v1";
    }
}
async function loadResumeSession() {
    const lastSessionId = localStorage.getItem("doomsday:lastSessionId") || "";
    if (!lastSessionId) {
        return;
    }
    try {
        await getSessionState(lastSessionId);
        resumeSessionId.value = lastSessionId;
    }
    catch {
        resumeSessionId.value = "";
    }
}
function goWorldFactory() {
    router.push("/world-factory");
}
function resumeGame() {
    if (!resumeSessionId.value) {
        return;
    }
    router.push(`/game/${resumeSessionId.value}`);
}
loadWorldVersion().catch(() => { });
onMounted(() => {
    loadResumeSession().catch(() => { });
});
async function startGame() {
    if (loading.value) {
        return;
    }
    loading.value = true;
    error.value = "";
    try {
        const data = await createSession(form);
        localStorage.setItem("doomsday:lastSessionId", data.sessionId);
        await router.push(`/game/${data.sessionId}`);
    }
    catch (e) {
        error.value = e instanceof Error ? e.message : "创建会话失败";
    }
    finally {
        loading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['difficulty-card']} */ ;
/** @type {__VLS_StyleScopedClasses['difficulty-card']} */ ;
/** @type {__VLS_StyleScopedClasses['difficulty-card']} */ ;
/** @type {__VLS_StyleScopedClasses['difficulty-card']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--cabin']} */ ;
/** @type {__VLS_StyleScopedClasses['boot-page']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-wrap boot-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel hero" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "meta-line" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "subtitle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel setup" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "32",
});
(__VLS_ctx.form.playerId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "difficulty-grid" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.difficultyOptions))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.form.difficulty = item.value;
            } },
        key: (item.value),
        ...{ class: "difficulty-card" },
        ...{ class: ({ active: __VLS_ctx.form.difficulty === item.value }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (item.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (item.desc);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "input" },
    maxlength: "64",
});
(__VLS_ctx.form.worldVersion);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.goWorldFactory) },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.router.push('/cabin');
        } },
    ...{ class: "btn btn--cabin" },
});
if (__VLS_ctx.resumeSessionId) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.resumeGame) },
        ...{ class: "btn" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.startGame) },
    ...{ class: "btn btn--accent" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? "创建中..." : "进入游戏");
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "error" },
    });
    (__VLS_ctx.error);
}
/** @type {__VLS_StyleScopedClasses['page-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['boot-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['hero']} */ ;
/** @type {__VLS_StyleScopedClasses['meta-line']} */ ;
/** @type {__VLS_StyleScopedClasses['subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['setup']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['difficulty-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['difficulty-card']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--cabin']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--accent']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            router: router,
            loading: loading,
            error: error,
            resumeSessionId: resumeSessionId,
            form: form,
            difficultyOptions: difficultyOptions,
            goWorldFactory: goWorldFactory,
            resumeGame: resumeGame,
            startGame: startGame,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=BootPage.vue.js.map
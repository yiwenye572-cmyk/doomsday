import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { chooseOption, getSessionState, submitTurn, useComebackCard } from "../api/game";
import { getDiary } from "../api/diary";
import { generateImage, gallerySearch } from "../api/media";
import ActionInput from "../components/game/ActionInput.vue";
import DiaryPanel from "../components/game/DiaryPanel.vue";
import NarrativePanel from "../components/game/NarrativePanel.vue";
import OptionsGrid from "../components/game/OptionsGrid.vue";
import StatusPanel from "../components/game/StatusPanel.vue";
const route = useRoute();
const router = useRouter();
const sessionId = computed(() => String(route.params.sessionId || ""));
const state = ref(null);
const plot = ref(null);
const image = ref(null);
const options = ref([]);
const diaryEntries = ref([]);
const diaryLoading = ref(false);
const diaryError = ref("");
const diaryLevel = ref("L0");
const loading = ref(false);
const pendingAction = ref(false);
const notice = ref("");
const headline = computed(() => `会话 ${sessionId.value}`);
async function refreshState() {
    if (!sessionId.value) {
        return;
    }
    state.value = await getSessionState(sessionId.value);
}
async function refreshDiary(level = diaryLevel.value) {
    if (!sessionId.value) {
        return;
    }
    diaryLevel.value = level;
    diaryLoading.value = true;
    diaryError.value = "";
    try {
        diaryEntries.value = await getDiary(sessionId.value, level);
    }
    catch (e) {
        diaryEntries.value = [];
        diaryError.value = e instanceof Error ? e.message : "日记加载失败";
    }
    finally {
        diaryLoading.value = false;
    }
}
async function handleSubmit(input) {
    if (!state.value || pendingAction.value) {
        return;
    }
    pendingAction.value = true;
    notice.value = "";
    try {
        const data = await submitTurn(sessionId.value, {
            expectedVersion: state.value.version,
            playerInput: input,
            clientTime: Date.now(),
        });
        plot.value = data.plot;
        // try to generate image for the new plot (backend may fallback)
        fetchImageForPlot(data.plot).catch(() => { });
        options.value = data.options;
        await refreshState();
        await refreshDiary();
    }
    catch (e) {
        await handleError(e);
    }
    finally {
        pendingAction.value = false;
    }
}
async function handleChoose(optionId) {
    if (!state.value || pendingAction.value) {
        return;
    }
    pendingAction.value = true;
    notice.value = "";
    try {
        const turn = state.value.turn;
        await chooseOption(sessionId.value, turn, {
            expectedVersion: state.value.version,
            optionId,
        });
        await refreshState();
        await refreshDiary();
        await fetchImageForPlot(plot.value);
    }
    catch (e) {
        await handleError(e);
    }
    finally {
        pendingAction.value = false;
    }
}
async function fetchImageForPlot(p) {
    image.value = null;
    if (!p || !p.text)
        return;
    try {
        const resp = await generateImage({ sessionId: sessionId.value, prompt: p.text, timeoutMs: 3000 });
        image.value = resp;
    }
    catch (err) {
        try {
            const gallery = await gallerySearch(p.text, 1);
            if (gallery && gallery.length) {
                image.value = gallery[0];
            }
        }
        catch (e) {
            // ignore, no image
        }
    }
}
async function handleComeback() {
    if (!state.value || pendingAction.value) {
        return;
    }
    pendingAction.value = true;
    notice.value = "";
    try {
        const result = await useComebackCard(sessionId.value, {
            expectedVersion: state.value.version,
        });
        notice.value = result.applied ? "翻盘卡已生效" : "翻盘卡未生效";
        await refreshState();
        await refreshDiary();
    }
    catch (e) {
        await handleError(e);
    }
    finally {
        pendingAction.value = false;
    }
}
async function handleError(error) {
    const message = error instanceof Error ? error.message : "请求失败";
    notice.value = message;
    if (message.toLowerCase().includes("version")) {
        await refreshState();
    }
}
async function init() {
    loading.value = true;
    notice.value = "";
    try {
        await refreshState();
        await refreshDiary("L0");
    }
    catch (e) {
        notice.value = e instanceof Error ? e.message : "状态加载失败";
    }
    finally {
        loading.value = false;
    }
}
function goReplay() {
    router.push(`/replay/${sessionId.value}`);
}
function onRegenerate() {
    fetchImageForPlot(plot.value).catch(() => { });
}
onMounted(init);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['layout']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-wrap game-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "panel topbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
(__VLS_ctx.headline);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "top-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.router.push('/');
        } },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.goReplay) },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.router.push('/admin');
        } },
    ...{ class: "btn btn--admin" },
});
if (!__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "layout" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "main-col" },
    });
    /** @type {[typeof NarrativePanel, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(NarrativePanel, new NarrativePanel({
        ...{ 'onRegenerate': {} },
        plot: (__VLS_ctx.plot),
        image: (__VLS_ctx.image),
    }));
    const __VLS_1 = __VLS_0({
        ...{ 'onRegenerate': {} },
        plot: (__VLS_ctx.plot),
        image: (__VLS_ctx.image),
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    let __VLS_3;
    let __VLS_4;
    let __VLS_5;
    const __VLS_6 = {
        onRegenerate: (__VLS_ctx.onRegenerate)
    };
    var __VLS_2;
    /** @type {[typeof ActionInput, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(ActionInput, new ActionInput({
        ...{ 'onSubmit': {} },
        ...{ 'onComeback': {} },
        loading: (__VLS_ctx.pendingAction),
    }));
    const __VLS_8 = __VLS_7({
        ...{ 'onSubmit': {} },
        ...{ 'onComeback': {} },
        loading: (__VLS_ctx.pendingAction),
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    let __VLS_10;
    let __VLS_11;
    let __VLS_12;
    const __VLS_13 = {
        onSubmit: (__VLS_ctx.handleSubmit)
    };
    const __VLS_14 = {
        onComeback: (__VLS_ctx.handleComeback)
    };
    var __VLS_9;
    /** @type {[typeof OptionsGrid, ]} */ ;
    // @ts-ignore
    const __VLS_15 = __VLS_asFunctionalComponent(OptionsGrid, new OptionsGrid({
        ...{ 'onChoose': {} },
        options: (__VLS_ctx.options),
        loading: (__VLS_ctx.pendingAction),
    }));
    const __VLS_16 = __VLS_15({
        ...{ 'onChoose': {} },
        options: (__VLS_ctx.options),
        loading: (__VLS_ctx.pendingAction),
    }, ...__VLS_functionalComponentArgsRest(__VLS_15));
    let __VLS_18;
    let __VLS_19;
    let __VLS_20;
    const __VLS_21 = {
        onChoose: (__VLS_ctx.handleChoose)
    };
    var __VLS_17;
    /** @type {[typeof DiaryPanel, ]} */ ;
    // @ts-ignore
    const __VLS_22 = __VLS_asFunctionalComponent(DiaryPanel, new DiaryPanel({
        ...{ 'onRefresh': {} },
        loading: (__VLS_ctx.diaryLoading),
        entries: (__VLS_ctx.diaryEntries),
        error: (__VLS_ctx.diaryError),
    }));
    const __VLS_23 = __VLS_22({
        ...{ 'onRefresh': {} },
        loading: (__VLS_ctx.diaryLoading),
        entries: (__VLS_ctx.diaryEntries),
        error: (__VLS_ctx.diaryError),
    }, ...__VLS_functionalComponentArgsRest(__VLS_22));
    let __VLS_25;
    let __VLS_26;
    let __VLS_27;
    const __VLS_28 = {
        onRefresh: (__VLS_ctx.refreshDiary)
    };
    var __VLS_24;
    /** @type {[typeof StatusPanel, ]} */ ;
    // @ts-ignore
    const __VLS_29 = __VLS_asFunctionalComponent(StatusPanel, new StatusPanel({
        state: (__VLS_ctx.state),
    }));
    const __VLS_30 = __VLS_29({
        state: (__VLS_ctx.state),
    }, ...__VLS_functionalComponentArgsRest(__VLS_29));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "panel loading-box" },
    });
}
if (__VLS_ctx.pendingAction && !__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice processing" },
    });
}
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
    });
    (__VLS_ctx.notice);
}
/** @type {__VLS_StyleScopedClasses['page-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['game-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['top-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--admin']} */ ;
/** @type {__VLS_StyleScopedClasses['layout']} */ ;
/** @type {__VLS_StyleScopedClasses['main-col']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['loading-box']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['processing']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ActionInput: ActionInput,
            DiaryPanel: DiaryPanel,
            NarrativePanel: NarrativePanel,
            OptionsGrid: OptionsGrid,
            StatusPanel: StatusPanel,
            router: router,
            state: state,
            plot: plot,
            image: image,
            options: options,
            diaryEntries: diaryEntries,
            diaryLoading: diaryLoading,
            diaryError: diaryError,
            loading: loading,
            pendingAction: pendingAction,
            notice: notice,
            headline: headline,
            refreshDiary: refreshDiary,
            handleSubmit: handleSubmit,
            handleChoose: handleChoose,
            handleComeback: handleComeback,
            goReplay: goReplay,
            onRegenerate: onRegenerate,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=GamePage.vue.js.map
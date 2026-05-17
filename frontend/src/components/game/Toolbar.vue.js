import { ref } from 'vue';
const __VLS_props = withDefaults(defineProps(), { saving: false });
const emit = defineEmits();
const fileInput = ref(null);
function triggerImport() {
    fileInput.value?.click();
}
function onFileChange(ev) {
    const file = ev.target.files?.[0];
    if (!file)
        return;
    const reader = new FileReader();
    reader.onload = (e) => emit('import', e.target?.result);
    reader.readAsText(file);
    ev.target.value = '';
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({ saving: false });
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.$emit('save');
        } },
    ...{ class: "btn" },
    disabled: (__VLS_ctx.saving),
});
(__VLS_ctx.saving ? '保存中…' : '💾 保存');
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.$emit('export');
        } },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.triggerImport) },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.$emit('undo');
        } },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.$emit('rest');
        } },
    ...{ class: "btn btn--danger" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.$emit('leave');
        } },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onFileChange) },
    ref: "fileInput",
    type: "file",
    accept: ".json",
    ...{ class: "hidden-input" },
});
/** @type {typeof __VLS_ctx.fileInput} */ ;
if (__VLS_ctx.lastSaved) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "saved-hint" },
    });
    (__VLS_ctx.lastSaved);
}
/** @type {__VLS_StyleScopedClasses['toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn--danger']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['hidden-input']} */ ;
/** @type {__VLS_StyleScopedClasses['saved-hint']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            fileInput: fileInput,
            triggerImport: triggerImport,
            onFileChange: onFileChange,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
; /* PartiallyEnd: #4569/main.vue */
//# sourceMappingURL=Toolbar.vue.js.map
function normalizeWord(input: string): string {
  return input.trim().toLowerCase();
}

export function buildFallbackExplanation(word: string): string {
  const normalized = normalizeWord(word);
  const syllables = normalized.split(/(?=[aeiouy])/i).filter((it) => it.length > 0).join("-") || normalized;

  return [
    `1) 单词与音节拆分：${normalized}（${syllables}）`,
    "2) 巧记法：",
    `- 把 ${normalized} 放进一个你常见的场景里，和画面绑定记忆。`,
    "- 读三遍并拼写一遍，做到“能读-能写-能想起”。",
    "3) 词源学讲解：建议拆分词根词缀或联想近形词进行对比记忆。",
    `4) 高频例句：I will remember "${normalized}" in context.（我会在语境中记住这个词。）`,
    "5) 24小时复习提示：10分钟后、睡前、次日早晨各复习1次。"
  ].join("\n");
}

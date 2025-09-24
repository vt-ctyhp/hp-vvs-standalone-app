export function normalizeHeaderName(name) {
  return name ? name.toString().trim().replace(/[_\s]+/g, ' ').toLowerCase() : '';
}

export function createHeaderMap(headers = []) {
  const headerRow = headers.slice();
  const normalizedToIndex = new Map();
  headerRow.forEach((header, index) => {
    normalizedToIndex.set(normalizeHeaderName(header), index);
  });

  function ensureHeader(name) {
    const normalized = normalizeHeaderName(name);
    if (!normalizedToIndex.has(normalized)) {
      headerRow.push(name);
      normalizedToIndex.set(normalized, headerRow.length - 1);
    }
  }

  function getIndex(name) {
    const normalized = normalizeHeaderName(name);
    if (normalizedToIndex.has(normalized)) {
      return normalizedToIndex.get(normalized);
    }
    return null;
  }

  function getOrThrow(name) {
    const index = getIndex(name);
    if (index === null || index === undefined) {
      throw new Error(`Header not found: ${name}`);
    }
    return index;
  }

  function getOrNull(name) {
    const index = getIndex(name);
    return index === undefined ? null : index;
  }

  function resolveAlias(names) {
    const candidates = Array.isArray(names) ? names : [names];
    for (const candidate of candidates) {
      const index = getIndex(candidate);
      if (index !== null && index !== undefined) {
        return index;
      }
    }
    return null;
  }

  function toRow(valuesByHeader = {}) {
    const row = new Array(headerRow.length).fill('');
    Object.entries(valuesByHeader).forEach(([header, value]) => {
      ensureHeader(header);
      const index = getIndex(header);
      row[index] = value;
    });
    return row;
  }

  return {
    headerRow,
    ensureHeader,
    getIndex,
    getOrThrow,
    getOrNull,
    resolveAlias,
    toRow,
  };
}

export function getValue(row, headerMap, aliases) {
  const index = headerMap.resolveAlias(aliases);
  if (index === null || index === undefined) {
    return null;
  }
  return row[index] ?? null;
}

export function setValue(row, headerMap, headerName, value) {
  headerMap.ensureHeader(headerName);
  const index = headerMap.getIndex(headerName);
  row[index] = value;
  return row;
}

export default createHeaderMap;

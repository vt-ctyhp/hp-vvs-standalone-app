import fs from 'node:fs';
import path from 'node:path';
import config from '../main/config.js';

const REQUIRED_SUBDIRS = ['03_Transcripts', '04_Summaries', '05_ChatLogs'];

function newestFile(files = []) {
  return files.sort((a, b) => b.mtimeMs - a.mtimeMs)[0] || null;
}

class DriveService {
  constructor(rootPath) {
    this.rootPath = rootPath || path.resolve('./drive');
  }

  clientFolder(rootApptId) {
    const folder = path.join(this.rootPath, rootApptId);
    fs.mkdirSync(folder, { recursive: true });
    for (const sub of REQUIRED_SUBDIRS) {
      fs.mkdirSync(path.join(folder, sub), { recursive: true });
    }
    return folder;
  }

  listFolder(folderPath) {
    if (!fs.existsSync(folderPath)) {
      return [];
    }
    return fs
      .readdirSync(folderPath)
      .map((name) => {
        const full = path.join(folderPath, name);
        const stats = fs.statSync(full);
        return { name, path: full, mtimeMs: stats.mtimeMs, stats };
      })
      .filter((entry) => entry.stats.isFile());
  }

  getNewestTranscript(rootApptId) {
    const folder = this.clientFolder(rootApptId);
    const transcripts = this.listFolder(path.join(folder, '03_Transcripts'));
    return newestFile(transcripts);
  }

  getNewestSummaryArtifact(rootApptId) {
    const folder = this.clientFolder(rootApptId);
    const summaries = this.listFolder(path.join(folder, '04_Summaries'));
    const candidates = summaries.filter((file) =>
      /__(summary|summary_corrected|analysis)__/.test(file.name.toLowerCase()) && file.name.endsWith('.json'),
    );
    return newestFile(candidates);
  }
}

let driveInstance;

export function getDriveService() {
  if (!driveInstance) {
    const root = config.get('LOCAL_DRIVE_ROOT', path.resolve('./drive'));
    driveInstance = new DriveService(root);
  }
  return driveInstance;
}

export default DriveService;

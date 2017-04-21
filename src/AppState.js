// @flow

type MentionData = {
  channelId: string,
  cardId: string,
  setCode: string
};

export default class AppState {
  lastMentioned: Array<MentionData>;

  constructor() {
    this.lastMentioned = [];
  }

  setLastMentioned: (channelId: string, cardId: string, setCode: string) => void;
  setLastMentioned(channelId: string, cardId: string, setCode: string) {
    this.lastMentioned = this.lastMentioned.filter(md => md.channelId !== channelId).concat([{
      channelId,
      cardId,
      setCode
    }]);
  }
}

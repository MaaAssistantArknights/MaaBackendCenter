package plus.maa.backend.common.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.entity.Copilot;

/**
 * @author LoMu
 * Date  2023-01-17 13:57
 */
@AllArgsConstructor
public class CopilotTypeCheck {

    /**
     * 数据校验
     * 如果数据不符合规范 则禁止传入 以防脏数据或导致作业无法顺利完成任务
     *
     * @param copilotDTO 上传内容
     */
    public static void copilotTypeCheck(CopilotDTO copilotDTO) {


        if (copilotDTO.getGroups() != null) {
            for (Copilot.Groups group : copilotDTO.getGroups()) {
                String name = group.getName();
                final String EXCEPTION_GROUP_PREFIX = "干员组[" + name + "]:\n";
                if (group.getOpers() == null) {
                    throw new MaaResultException(EXCEPTION_GROUP_PREFIX + "干员不可为空");
                }
            }
        }

        if (copilotDTO.getActions() != null) {
            for (Copilot.Action actions : copilotDTO.getActions()) {
                ActionsType actionsType = ActionsType.checkActionsType(actions.getType());
                DirectionType directionType = DirectionType.checkDirectionType(actions.getDirection());
                String actionName = actions.getName();
                Integer[] location = actions.getLocation();
                final String EXCEPTION_ACTION_PREFIX = "错误干员或干员组[" + actionName + "]:\n";


                if (actionsType == ActionsType.SKILLUSAGE) {
                    if (actions.getSkillUsage() == 0) {
                        throw new MaaResultException(EXCEPTION_ACTION_PREFIX + "当动作类型为技能用法时,技能用法该选项必选");
                    }
                }

                if (location != null) {
                    if (location.length != 2) {
                        throw new MaaResultException(EXCEPTION_ACTION_PREFIX + "干员位置的数据格式不符合规定");
                    }
                    if (location[0] > 9 || location[0] < 0) {
                        throw new MaaResultException(EXCEPTION_ACTION_PREFIX + "干员位置X 坐标超出地图范围 (0-9)");
                    }
                    if (location[1] > 6 || location[1] < 0) {
                        throw new MaaResultException(EXCEPTION_ACTION_PREFIX + "干员位置Y 坐标超出地图范围 (0-6)");
                    }
                }
                if (actionsType == ActionsType.DEPLOY && (directionType == DirectionType.NONE || location == null)) {
                    throw new MaaResultException(EXCEPTION_ACTION_PREFIX + "当动作类型为部署时,干员位置或干员朝向不可为空");
                }

            }
        }
    }

    @Getter
    public enum DirectionType {
        LEFT("左"),
        RIGHT("右"),
        UP("上"),
        DOWN("下"),
        NONE("无"),

        TYPEEXCTION("干员朝向数据格式不合法,请通过作业编辑器进行合法的作业编辑");
        private final String display;

        DirectionType(String display) {
            this.display = display;
        }


        /**
         * 检验数据格式 大小写敏感
         *
         * @param type type
         * @return DirectionType
         */
        public static DirectionType checkDirectionType(String type) {
            DirectionType directionType = switch (type) {
                case "Left", "左" -> LEFT;
                case "Right", "右" -> RIGHT;
                case "Up", "上" -> UP;
                case "Down", "下" -> DOWN;
                case "None", "无" -> NONE;
                default -> TYPEEXCTION;
            };
            if (directionType == DirectionType.TYPEEXCTION) {
                throw new MaaResultException(directionType.getDisplay());
            }
            return directionType;
        }

    }

    @Getter
    public enum ActionsType {
        DEPLOY("部署"),
        SKILL("技能"),
        RETREAT("撤退"),
        SPEEDUP("二倍速"),
        BULLETTIME("子弹时间"),
        SKILLUSAGE("技能用法"),
        OUTPUT("打印"),
        SKILLDAEMON("摆烂挂机"),
        MOVECAMERA("移动镜头"),

        TYPEEXCTION("动作类型数据格式不合法,请通过作业编辑器进行合法的作业编辑");

        private final String display;

        ActionsType(String display) {
            this.display = display;
        }

        /**
         * 检验数据格式 大小写敏感
         *
         * @param type type
         * @return ActionsType
         */
        public static ActionsType checkActionsType(String type) {
            ActionsType actionsType = switch (type) {
                case "Deploy", "部署" -> DEPLOY;
                case "Skill", "技能" -> SKILL;
                case "Retreat", "撤退" -> RETREAT;
                case "SpeedUp", "二倍速" -> SPEEDUP;
                case "BulletTime", "子弹时间" -> BULLETTIME;
                case "SkillUsage", "技能用法" -> SKILLUSAGE;
                case "Output", "打印" -> OUTPUT;
                case "SkillDaemon", "摆烂挂机" -> SKILLDAEMON;
                case "Movecamera", "移动镜头" -> MOVECAMERA;
                default -> TYPEEXCTION;
            };
            if (actionsType == ActionsType.TYPEEXCTION) {
                throw new MaaResultException(actionsType.getDisplay());
            }
            return actionsType;
        }
    }

    @Getter
    public enum RatingType {
        LIKE(1),
        DISLIKE(2),
        NONE(0);

        private final Integer display;

        RatingType(Integer display) {
            this.display = display;
        }

        public static RatingType ratingTypeCheck(String type) {
            if (StringUtils.isBlank(type)) type = "None";
            return switch (type) {
                case "Like" -> LIKE;
                case "Dislike" -> DISLIKE;
                case "None" -> NONE;
                default -> NONE;
            };
        }
    }

}
